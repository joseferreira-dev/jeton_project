package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Comprovante;
import br.com.cremepe.jeton.servico.ComprovanteService;
import br.com.cremepe.jeton.servico.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired private ComprovanteService comprovanteService;
    @Autowired private FileStorageService fileStorageService;

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadFicheiro(@PathVariable Integer id, HttpServletRequest request) {
        
        try {
            // 1. Busca os metadados na base de dados
            Comprovante comprovante = comprovanteService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Comprovante não encontrado na base de dados."));

            // 2. Busca o ficheiro localmente ou através de Fallback no FTP
            Resource resource = fileStorageService.loadFileAsResource(comprovante.getNomeArquivo(), comprovante.getAno(), comprovante.getMes());

            // 3. Define o Content-Type
            String contentType = comprovante.getContentType();
            if(contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + comprovante.getNomeComprovante() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            // Se o ficheiro físico não existir localmente (apagado ou no FTP antigo), 
            // devolvemos um HTML elegante para ser exibido dentro do iframe do Modal.
            String htmlErro = "<html><body style='font-family: Arial, sans-serif; text-align: center; padding-top: 20%; color: #fff; background-color: #333;'>" +
                              "<h2><span style='font-size: 50px;'>📄❌</span><br><br>Documento Indisponível</h2>" +
                              "<p style='color: #ccc;'>O ficheiro físico não foi encontrado.</p>" +
                              "</body></html>";
                              
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlErro);
        }
    }
}