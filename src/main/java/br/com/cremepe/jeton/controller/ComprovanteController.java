package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.Comprovante;
import br.com.cremepe.jeton.service.ComprovanteService;
import br.com.cremepe.jeton.service.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/comprovantes")
@PreAuthorize("hasAuthority('C') or hasAuthority('S')")
public class ComprovanteController {

    private static final Logger log = LoggerFactory.getLogger(ComprovanteController.class);

    private final ComprovanteService comprovanteService;
    private final FileStorageService fileStorageService;

    ComprovanteController(ComprovanteService comprovanteService, FileStorageService fileStorageService) {
        this.comprovanteService = comprovanteService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadFicheiro(@PathVariable Integer id) {
        try {
            Comprovante comprovante = comprovanteService.buscarPorId(id)
                    .orElseThrow(() -> new EntityNotFoundException("Comprovante não encontrado na base de dados."));

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

        } catch (EntityNotFoundException e) {
            log.warn("Comprovante não encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body(gerarPaginaErro("Documento Indisponível", "O comprovante solicitado não foi encontrado."));
        } catch (Exception e) {
            log.error("Erro ao tentar baixar comprovante ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_HTML)
                    .body(gerarPaginaErro("Erro Interno", "Ocorreu um erro ao tentar acessar o documento."));
        }
    }

    private String gerarPaginaErro(String titulo, String mensagem) {
        return """
                <html>
                <head><title>Erro - Jeton</title></head>
                <body style='font-family: Arial, sans-serif; text-align: center; padding-top: 20%; color: #fff; background-color: #333;'>
                    <h2><span style='font-size: 50px;'>📄❌</span><br><br>%s</h2>
                    <p style='color: #ccc;'>%s</p>
                </body>
                </html>
                """
                .formatted(titulo, mensagem);
    }
}