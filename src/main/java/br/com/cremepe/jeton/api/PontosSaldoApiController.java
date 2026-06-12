package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.dto.PontosRemanescentesDTO;
import br.com.cremepe.jeton.service.JetonService;
import br.com.cremepe.jeton.service.PontosSaldoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pontos-saldo")
@PreAuthorize("isAuthenticated()")
public class PontosSaldoApiController {

    private final JetonService jetonService;
    private final PontosSaldoService pontosSaldoService;

    PontosSaldoApiController(JetonService jetonService, PontosSaldoService pontosSaldoService) {
        this.jetonService = jetonService;
        this.pontosSaldoService = pontosSaldoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('P') or hasAuthority('S')")
    public ResponseEntity<List<PontosRemanescentesDTO>> obterResumoPontos() {
        return ResponseEntity.ok(jetonService.listarSaldosAgrupados());
    }

    @GetMapping("/conselheiro/{idConselheiro}")
    @PreAuthorize("hasAuthority('P') or hasAuthority('S') or #idConselheiro == authentication.principal.viewUserLogin.idPessoa")
    public ResponseEntity<PontosRemanescentesDTO> obterSaldoConselheiro(@PathVariable Integer idConselheiro) {
        PontosRemanescentesDTO dto = jetonService.obterSaldoPorConselheiro(idConselheiro);
        return ResponseEntity.ok(dto);
    }
}