package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Comprovante;
import br.com.cremepe.jeton.domain.TipoAnexo;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.TipoAnexoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class ComprovanteService {

    private static final Logger log = LoggerFactory.getLogger(ComprovanteService.class);

    private final ComprovanteRepository comprovanteRepository;
    private final TipoAnexoRepository tipoAnexoRepository;
    private final FileStorageService fileStorageService;
    private final LogJetonService logJetonService;

    public ComprovanteService(ComprovanteRepository comprovanteRepository,
            TipoAnexoRepository tipoAnexoRepository,
            FileStorageService fileStorageService,
            LogJetonService logJetonService) {
        this.comprovanteRepository = comprovanteRepository;
        this.tipoAnexoRepository = tipoAnexoRepository;
        this.fileStorageService = fileStorageService;
        this.logJetonService = logJetonService;
    }

    @Transactional
    public Comprovante criarComprovante(MultipartFile file, Integer idTipoAnexo, String descricaoUsuario) {
        validarArquivo(file);
        YearMonth dataAtual = obterDataAtual();
        int ano = dataAtual.getYear();
        int mes = dataAtual.getMonthValue();

        String nomeArquivoGerado = fileStorageService.salvarArquivoNoFtp(file, ano, mes);
        log.debug("Arquivo enviado para FTP: {}", nomeArquivoGerado);

        TipoAnexo tipo = tipoAnexoRepository.findById(idTipoAnexo)
                .orElseThrow(() -> new RuntimeException("Tipo de anexo inválido. ID: " + idTipoAnexo));

        Comprovante comprovante = new Comprovante();
        comprovante.setTipoAnexo(tipo);
        comprovante.setNomeComprovante(descricaoUsuario.trim());
        comprovante.setNomeArquivo(nomeArquivoGerado);
        comprovante.setContentType(obterContentTypeValido(file.getContentType()));
        comprovante.setMes(mes);
        comprovante.setAno(ano);

        Comprovante salvo = comprovanteRepository.save(comprovante);
        log.info("Novo comprovante criado: ID={}, nome='{}', arquivo={}, tipo={}, ano/mês={}/{}",
                salvo.getIdComprovante(), salvo.getNomeComprovante(), salvo.getNomeArquivo(),
                tipo.getNome(), salvo.getAno(), salvo.getMes());

        logJetonService.logComprovanteCriado(salvo);
        return salvo;
    }

    @Transactional(readOnly = true)
    public Optional<Comprovante> buscarPorId(Integer id) {
        return comprovanteRepository.findById(id);
    }

    @Transactional
    public void excluirComprovante(Integer id) {
        comprovanteRepository.findById(id).ifPresent(comp -> {
            Comprovante copia = copiarComprovante(comp);

            fileStorageService.excluirArquivo(comp.getNomeArquivo(), comp.getAno(), comp.getMes());
            comprovanteRepository.delete(comp);

            logJetonService.logComprovanteExcluido(copia);
            log.info("Comprovante excluído: ID={}, nome='{}', arquivo={}", id, comp.getNomeComprovante(),
                    comp.getNomeArquivo());
        });
    }

    private YearMonth obterDataAtual() {
        return YearMonth.now();
    }

    private void validarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Arquivo vazio ou nulo.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new RuntimeException("Nome do arquivo inválido.");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("Arquivo excede o tamanho máximo permitido (10 MB).");
        }
        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        if (!extension.matches("pdf|jpg|jpeg|png")) {
            throw new RuntimeException("Formato de arquivo não permitido. Use PDF, JPG ou PNG.");
        }
    }

    private String obterContentTypeValido(String contentType) {
        if (contentType == null)
            return Comprovante.CONTENT_TYPE_FALLBACK;
        if (contentType.equalsIgnoreCase("application/pdf"))
            return Comprovante.CONTENT_TYPE_PDF;
        if (contentType.startsWith("image/"))
            return Comprovante.CONTENT_TYPE_IMAGE;
        return Comprovante.CONTENT_TYPE_FALLBACK;
    }

    @Transactional(readOnly = true)
    public List<Comprovante> listarTodos() {
        return comprovanteRepository.findAll();
    }

    private Comprovante copiarComprovante(Comprovante original) {
        Comprovante copia = new Comprovante();
        copia.setIdComprovante(original.getIdComprovante());
        copia.setTipoAnexo(original.getTipoAnexo());
        copia.setNomeComprovante(original.getNomeComprovante());
        copia.setNomeArquivo(original.getNomeArquivo());
        copia.setContentType(original.getContentType());
        copia.setMes(original.getMes());
        copia.setAno(original.getAno());
        return copia;
    }
}