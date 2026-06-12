package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.GestaoConselheiro;
import br.com.cremepe.jeton.dto.ConselheiroDTO;
import br.com.cremepe.jeton.dto.GestaoConselheiroDTO;
import br.com.cremepe.jeton.dto.GestaoDTO;
import br.com.cremepe.jeton.mapper.ConselheiroMapper;
import br.com.cremepe.jeton.mapper.GestaoConselheiroMapper;
import br.com.cremepe.jeton.mapper.GestaoMapper;
import br.com.cremepe.jeton.service.GestaoConselheiroService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gestao-conselheiros")
@PreAuthorize("isAuthenticated()")
public class GestaoConselheiroApiController {

    private final GestaoConselheiroService gestaoConselheiroService;
    private final GestaoConselheiroMapper gestaoConselheiroMapper;
    private final ConselheiroMapper conselheiroMapper;
    private final GestaoMapper gestaoMapper;

    GestaoConselheiroApiController(GestaoConselheiroService gestaoConselheiroService,
            ConselheiroMapper conselheiroMapper, GestaoConselheiroMapper gestaoConselheiroMapper,
            GestaoMapper gestaoMapper) {
        this.gestaoConselheiroService = gestaoConselheiroService;
        this.gestaoConselheiroMapper = gestaoConselheiroMapper;
        this.conselheiroMapper = conselheiroMapper;
        this.gestaoMapper = gestaoMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('G') or hasAuthority('S')")
    public ResponseEntity<List<GestaoConselheiroDTO>> listarTodos() {
        List<GestaoConselheiroDTO> dtos = gestaoConselheiroService.listarTodos().stream()
                .map(gestaoConselheiroMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/gestao/{idGestao}/conselheiros")
    @PreAuthorize("hasAuthority('G') or hasAuthority('S')")
    public ResponseEntity<List<ConselheiroDTO>> listarConselheirosPorGestao(
            @PathVariable Integer idGestao,
            @RequestParam(defaultValue = "true") boolean ativos) {
        List<GestaoConselheiro> vinculos;
        if (ativos) {
            vinculos = gestaoConselheiroService.listarPorGestaoAtivos(idGestao);
        } else {
            vinculos = gestaoConselheiroService.listarPorGestaoInativos(idGestao);
        }
        List<ConselheiroDTO> conselheiros = vinculos.stream()
                .map(GestaoConselheiro::getConselheiro)
                .map(conselheiroMapper::toDto)
                .toList();
        return ResponseEntity.ok(conselheiros);
    }

    @GetMapping("/conselheiro/{idConselheiro}/gestoes")
    @PreAuthorize("hasAuthority('G') or hasAuthority('S') or #idConselheiro == authentication.principal.viewUserLogin.idPessoa")
    public ResponseEntity<List<GestaoDTO>> listarGestoesPorConselheiro(
            @PathVariable Integer idConselheiro,
            @RequestParam(defaultValue = "true") boolean ativos) {
        List<GestaoConselheiro> vinculos;
        if (ativos) {
            vinculos = gestaoConselheiroService.listarPorConselheiroAtivos(idConselheiro);
        } else {
            vinculos = gestaoConselheiroService.listarPorConselheiroInativos(idConselheiro);
        }
        List<GestaoDTO> gestoes = vinculos.stream()
                .map(GestaoConselheiro::getGestao)
                .map(gestaoMapper::toDto)
                .toList();
        return ResponseEntity.ok(gestoes);
    }

    @GetMapping("/existe")
    @PreAuthorize("hasAuthority('G') or hasAuthority('S')")
    public ResponseEntity<Boolean> existeVinculo(
            @RequestParam Integer idGestao,
            @RequestParam Integer idConselheiro,
            @RequestParam(required = false) Boolean ativos) {

        var vinculo = gestaoConselheiroService.buscarPorId(idGestao, idConselheiro);

        if (vinculo.isEmpty()) {
            return ResponseEntity.ok(false);
        }
        if (ativos == null) {
            return ResponseEntity.ok(true);
        }

        String situacaoEsperada = ativos ? GestaoConselheiro.SITUACAO_ATIVO : GestaoConselheiro.SITUACAO_INATIVO;
        boolean situacaoCorreta = situacaoEsperada.equals(vinculo.get().getInSituacao());

        return ResponseEntity.ok(situacaoCorreta);
    }
}