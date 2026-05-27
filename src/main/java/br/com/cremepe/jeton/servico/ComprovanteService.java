package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Comprovante;
import br.com.cremepe.jeton.dominio.TipoAnexo;
import br.com.cremepe.jeton.repositorio.ComprovanteRepository;
import br.com.cremepe.jeton.repositorio.TipoAnexoRepository;
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
    @Autowired
    private LogJetonService logJetonService;

    /**
     * Cria um novo comprovante a partir do arquivo enviado.
     * O arquivo é armazenado no FTP e os metadados salvos no banco.
     * A data utilizada para a organização das pastas é a data atual do servidor.
     */
    @Transactional
    public Comprovante guardarComprovante(MultipartFile file, Integer idTipoAnexo,
            String descricaoUsuario, Integer idUsuarioLogado) {
        validarArquivo(file);

        YearMonth dataAtual = obterDataAtual();
        int ano = dataAtual.getYear();
        int mes = dataAtual.getMonthValue();

        // 1. Envia o ficheiro para o FTP e obtém o nome único
        String nomeArquivoGerado = fileStorageService.storeFileToFtp(file, ano, mes);
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

        // Auditoria
        String textoLog = String.format(
                "Comprovante criado: ID=%d, Nome='%s', Tipo='%s', Ano=%d, Mês=%d, Arquivo=%s",
                salvo.getIdComprovante(), salvo.getNomeComprovante(), tipo.getNome(),
                salvo.getAno(), salvo.getMes(), salvo.getNomeArquivo());
        logJetonService.registrarLog("comprovante", idUsuarioLogado, textoLog);

        return salvo;
    }

    @Transactional(readOnly = true)
    public Optional<Comprovante> buscarPorId(Integer id) {
        return comprovanteRepository.findById(id);
    }

    @Transactional
    public void excluirComprovante(Integer id, Integer idUsuarioLogado) {
        comprovanteRepository.findById(id).ifPresent(comp -> {
            // Coleta informações para o log antes de excluir
            String nomeComprovante = comp.getNomeComprovante();
            String nomeArquivo = comp.getNomeArquivo();
            String tipoAnexo = comp.getTipoAnexo().getNome();
            int ano = comp.getAno();
            int mes = comp.getMes();

            // Remove o arquivo físico do FTP
            fileStorageService.deleteFile(comp.getNomeArquivo(), comp.getAno(), comp.getMes());
            // Remove o registro do banco
            comprovanteRepository.delete(comp);

            log.info("Comprovante excluído: ID={}, nome='{}', arquivo={}", id, nomeComprovante, nomeArquivo);

            String textoLog = String.format(
                    "Comprovante excluído: ID=%d, Nome='%s', Tipo='%s', Ano=%d, Mês=%d, Arquivo=%s",
                    id, nomeComprovante, tipoAnexo, ano, mes, nomeArquivo);
            logJetonService.registrarLog("comprovante", idUsuarioLogado, textoLog);
        });
    }

    @Transactional
    public Comprovante atualizar(Comprovante comprovante, Integer idUsuarioLogado) {
        Comprovante existente = comprovanteRepository.findById(comprovante.getIdComprovante())
                .orElseThrow(() -> new RuntimeException("Comprovante não encontrado para atualização"));

        // Registra alterações relevantes (exemplo: nome do comprovante)
        String nomeAntigo = existente.getNomeComprovante();
        String nomeNovo = comprovante.getNomeComprovante();

        Comprovante atualizado = comprovanteRepository.save(comprovante);

        if (!nomeAntigo.equals(nomeNovo)) {
            log.info("Comprovante ID {} teve nome alterado de '{}' para '{}'",
                    atualizado.getIdComprovante(), nomeAntigo, nomeNovo);
            String textoLog = String.format(
                    "Comprovante ID %d atualizado: nome alterado de '%s' para '%s'",
                    atualizado.getIdComprovante(), nomeAntigo, nomeNovo);
            logJetonService.registrarLog("comprovante", idUsuarioLogado, textoLog);
        } else {
            // Log genérico se outras propriedades forem alteradas (ex: tipo, etc.)
            log.debug("Comprovante ID {} atualizado (possíveis alterações não mapeadas)",
                    atualizado.getIdComprovante());
            String textoLog = String.format("Comprovante ID %d atualizado (detalhes não especificados)",
                    atualizado.getIdComprovante());
            logJetonService.registrarLog("comprovante", idUsuarioLogado, textoLog);
        }

        return atualizado;
    }

    // =========================================================================
    // MÉTODOS PRIVADOS AUXILIARES
    // =========================================================================
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
        // Tamanho máximo (ex: 10 MB) – ajuste conforme necessidade
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("Arquivo excede o tamanho máximo permitido (10 MB).");
        }
        // Extensão permitida
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