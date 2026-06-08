package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.domain.Comprovante;
import br.com.cremepe.jeton.servico.ComprovanteService;
import br.com.cremepe.jeton.servico.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/comprovantes")
public class ComprovanteController {

    private static final Logger log = LoggerFactory.getLogger(ComprovanteController.class);

    @Autowired
    private ComprovanteService comprovanteService;
    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadFicheiro(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Comprovante comprovante = comprovanteService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Comprovante não encontrado na base de dados."));

            Resource resource = fileStorageService.carregarArquivo(
                    comprovante.getNomeArquivo(),
                    comprovante.getAno(),
                    comprovante.getMes());

            String contentType = comprovante.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(resource.contentLength())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + comprovante.getNomeComprovante() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Erro ao tentar baixar comprovante ID {}: {}", id, e.getMessage());
            String htmlErro = gerarPaginaErro("Documento Indisponível",
                    "O ficheiro físico não foi encontrado ou não pode ser acessado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlErro);
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================
    private String gerarPaginaErro(String titulo, String mensagem) {
        return "<html><body style='font-family: Arial, sans-serif; text-align: center; padding-top: 20%; color: #fff; background-color: #333;'>"
                + "<h2><span style='font-size: 50px;'>📄❌</span><br><br>" + titulo + "</h2>"
                + "<p style='color: #ccc;'>" + mensagem + "</p>"
                + "</body></html>";
    }
}