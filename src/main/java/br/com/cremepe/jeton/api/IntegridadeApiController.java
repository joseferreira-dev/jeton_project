package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.service.IntegridadeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integridade")
@PreAuthorize("hasAuthority('S')")
public class IntegridadeApiController {

    private final IntegridadeService integridadeService;

    public IntegridadeApiController(IntegridadeService integridadeService) {
        this.integridadeService = integridadeService;
    }

    @PostMapping("/verificar")
    public ResponseEntity<String> verificar() {
        integridadeService.executarVerificacaoManual();
        return ResponseEntity.ok("Verificação de integridade executada. Consulte os logs para detalhes.");
    }

    @GetMapping("/orfaos/download")
    public ResponseEntity<byte[]> downloadOrfaos() {
        byte[] zip = integridadeService.downloadComprovantesOrfaos();
        if (zip == null || zip.length == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comprovantes_orfãos.zip\"")
                .body(zip);
    }

    @DeleteMapping("/orfaos")
    public ResponseEntity<String> excluirOrfaos() {
        String resultado = integridadeService.excluirComprovantesOrfaos();
        return ResponseEntity.ok(resultado);
    }
}