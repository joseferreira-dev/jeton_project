package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.domain.Comprovante;
import br.com.cremepe.jeton.domain.TipoAnexo;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.TipoAnexoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.Optional;

@Service
public class ComprovanteService {

    private static final Logger log = LoggerFactory.getLogger(ComprovanteService.class);

    @Autowired
    private ComprovanteRepository comprovanteRepository;
    @Autowired
    private TipoAnexoRepository tipoAnexoRepository;
    @Autowired
    private FileStorageService fileStorageService;

    @Auditar(tabela = "comprovante", acao = "CRIAR", descricao = "Criação de novo comprovante", dadosParametros = "{ 'nomeOriginal': #file.originalFilename, 'tamanho': #file.size, 'ano': #ano, 'mes': #mes, 'idTipoAnexo': #idTipoAnexo, 'descricaoUsuario': #descricaoUsuario }", dadosRetorno = "#result", auditarExcecao = true)
    @Transactional
    public Comprovante criarComprovante(MultipartFile file, Integer idTipoAnexo,
            String descricaoUsuario) {
        validarArquivo(file);
        YearMonth dataAtual = obterDataAtual();
        int ano = dataAtual.getYear();
        int mes = dataAtual.getMonthValue();

        // 1. Envia o ficheiro para o FTP e obtém o nome único
        String nomeArquivoGerado = fileStorageService.salvarArquivoNoFtp(file, ano, mes);
        log.debug("Arquivo enviado para FTP: {}", nomeArquivoGerado);

        // 2. Busca o Tipo de Anexo
        TipoAnexo tipo = tipoAnexoRepository.findById(idTipoAnexo)
                .orElseThrow(() -> new RuntimeException("Tipo de anexo inválido. ID: " + idTipoAnexo));

        // 3. Monta a entidade Comprovante
        Comprovante comprovante = new Comprovante();
        comprovante.setTipoAnexo(tipo);
        comprovante.setNomeComprovante(descricaoUsuario.trim());
        comprovante.setNomeArquivo(nomeArquivoGerado);
        comprovante.setContentType(obterContentTypeValido(file.getContentType()));
        comprovante.setMes(mes);
        comprovante.setAno(ano);

        // 4. Salva os metadados
        Comprovante salvo = comprovanteRepository.save(comprovante);
        log.info("Novo comprovante criado: ID={}, nome='{}', arquivo={}, tipo={}, ano/mês={}/{}",
                salvo.getIdComprovante(), salvo.getNomeComprovante(), salvo.getNomeArquivo(),
                tipo.getNome(), salvo.getAno(), salvo.getMes());
        return salvo;
    }

    @Transactional(readOnly = true)
    public Optional<Comprovante> buscarPorId(Integer id) {
        return comprovanteRepository.findById(id);
    }

    @Auditar(tabela = "comprovante", acao = "EXCLUIR", descricao = "Exclusão de comprovante", dadosParametros = "{ 'idComprovante': #id }", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public void excluirComprovante(Integer id) {
        comprovanteRepository.findById(id).ifPresent(comp -> {
            // Remove o arquivo físico do FTP
            fileStorageService.excluirArquivo(comp.getNomeArquivo(), comp.getAno(), comp.getMes());
            // Remove o registro do banco
            comprovanteRepository.delete(comp);
            log.info("Comprovante excluído: ID={}, nome='{}', arquivo={}", id, comp.getNomeComprovante(),
                    comp.getNomeArquivo());
        });
    }

    @Auditar(tabela = "comprovante", acao = "ATUALIZAR", descricao = "Atualização de comprovante (ex: nome)", dadosParametros = "{ 'idComprovante': #comprovante.idComprovante, 'nomeComprovante': #comprovante.nomeComprovante }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public Comprovante atualizarComprovante(Comprovante comprovante) {
        Comprovante existente = comprovanteRepository.findById(comprovante.getIdComprovante())
                .orElseThrow(() -> new RuntimeException("Comprovante não encontrado para atualização"));

        // Atualiza apenas campos permitidos (ex: nomeComprovante)
        existente.setNomeComprovante(comprovante.getNomeComprovante());
        // Se outros campos forem permitidos, atualize aqui

        Comprovante atualizado = comprovanteRepository.save(existente);
        log.info("Comprovante atualizado: ID={}, novo nome='{}'", atualizado.getIdComprovante(),
                atualizado.getNomeComprovante());
        return atualizado;
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
}