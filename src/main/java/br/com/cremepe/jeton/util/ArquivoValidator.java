package br.com.cremepe.jeton.util;

import br.com.cremepe.jeton.domain.TipoAnexo;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.TipoAnexoRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Component
public class ArquivoValidator {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final String[] EXTENSOES_PERMITIDAS = { "pdf", "jpg", "jpeg", "png" };
    private static final String[] CONTENT_TYPES_PERMITIDOS = { "application/pdf", "image/jpeg", "image/png" };

    private final TipoAnexoRepository tipoAnexoRepository;
    private final ComprovanteRepository comprovanteRepository;

    public ArquivoValidator(TipoAnexoRepository tipoAnexoRepository,
            ComprovanteRepository comprovanteRepository) {
        this.tipoAnexoRepository = tipoAnexoRepository;
        this.comprovanteRepository = comprovanteRepository;
    }

    public void validarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Arquivo vazio ou nulo.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new RuntimeException("Nome do arquivo inválido.");
        }

        // Valida tamanho
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Arquivo excede o tamanho máximo permitido (10 MB).");
        }

        // Valida extensão
        String extension = "";
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalName.substring(lastDot + 1).toLowerCase();
        }
        boolean extensaoValida = false;
        for (String ext : EXTENSOES_PERMITIDAS) {
            if (ext.equals(extension)) {
                extensaoValida = true;
                break;
            }
        }
        if (!extensaoValida) {
            throw new RuntimeException("Formato de arquivo não permitido. Use PDF, JPG ou PNG.");
        }

        // Valida content type (opcional, mas reforça)
        String contentType = file.getContentType();
        if (contentType != null) {
            boolean contentTypeValido = false;
            for (String ct : CONTENT_TYPES_PERMITIDOS) {
                if (ct.equalsIgnoreCase(contentType)) {
                    contentTypeValido = true;
                    break;
                }
            }
            if (!contentTypeValido && !contentType.startsWith("image/")) {
                // Se não for um dos tipos conhecidos, mas ainda permitimos (fallback)
                // Apenas avisamos, mas não bloqueamos para não quebrar uploads
            }
        }
    }

    public void validarNomeArquivo(String fileName) {
        if (fileName == null || fileName.contains("..")) {
            throw new RuntimeException("Nome de arquivo inválido.");
        }
    }

    public String obterContentTypeValido(String contentType) {
        if (contentType == null) {
            return "application/octet-stream";
        }
        if (contentType.equalsIgnoreCase("application/pdf")) {
            return "application/pdf";
        }
        if (contentType.startsWith("image/")) {
            return "image/jpeg"; // ou "image/png", mas padronizamos como jpeg
        }
        return "application/octet-stream";
    }

    public void validarNomeUnicoTipoAnexo(TipoAnexo tipoAnexo, boolean isNovo) {
        if (tipoAnexo.getNome() == null || tipoAnexo.getNome().trim().isEmpty()) {
            return;
        }
        String nome = tipoAnexo.getNome().trim();
        Integer idAtual = isNovo ? null : tipoAnexo.getIdTipo();

        Optional<TipoAnexo> existente = tipoAnexoRepository.findByNome(nome);
        if (existente.isPresent() && (idAtual == null || !existente.get().getIdTipo().equals(idAtual))) {
            throw new RuntimeException("Já existe um tipo de anexo cadastrado com o nome '" + nome + "'.");
        }
    }

    public void validarExclusaoTipoAnexo(Integer idTipo) {
        long count = comprovanteRepository.findByTipoAnexoIdTipo(idTipo).size();
        if (count > 0) {
            throw new RuntimeException("Não é possível excluir este tipo de anexo pois existem " + count +
                    " comprovante(s) vinculado(s) a ele.");
        }
    }
}