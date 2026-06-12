package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.dto.GestaoDTO;
import br.com.cremepe.jeton.mapper.GestaoMapper;
import br.com.cremepe.jeton.service.GestaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gestoes")
@PreAuthorize("isAuthenticated()")
public class GestaoApiController {

    private final GestaoService gestaoService;
    private final GestaoMapper gestaoMapper;

    GestaoApiController(GestaoService gestaoService, GestaoMapper gestaoMapper) {
        this.gestaoService = gestaoService;
        this.gestaoMapper = gestaoMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('G') or hasAuthority('S')")
    public ResponseEntity<List<GestaoDTO>> listarTodas() {
        List<GestaoDTO> dtos = gestaoService.listarTodos().stream()
                .map(gestaoMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('G') or hasAuthority('S')")
    public ResponseEntity<Page<GestaoDTO>> listarPaginado(
            @RequestParam(required = false, defaultValue = "") String termo,
            @PageableDefault(size = 10, sort = "nomeGestao") Pageable pageable) {
        Page<Gestao> page = gestaoService.listarComPaginacaoEPesquisa(termo, pageable.getPageNumber(),
                pageable.getPageSize(), pageable.getSort().get().findFirst().get().getProperty(),
                pageable.getSort().get().findFirst().get().isAscending() ? "asc" : "desc");
        Page<GestaoDTO> dtoPage = page.map(gestaoMapper::toDto);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('G') or hasAuthority('S')")
    public ResponseEntity<GestaoDTO> buscarPorId(@PathVariable Integer id) {
        Gestao gestao = gestaoService.buscarGestaoOuFalhar(id);
        return ResponseEntity.ok(gestaoMapper.toDto(gestao));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<GestaoDTO> criar(@Valid @RequestBody GestaoDTO gestaoDTO) {
        Gestao gestao = new Gestao();
        gestao.setNomeGestao(gestaoDTO.nome());
        gestao.setDtInicio(gestaoDTO.dataInicio());
        gestao.setDtFim(gestaoDTO.dataFim());
        Gestao salva = gestaoService.criarGestao(gestao);
        return ResponseEntity.status(HttpStatus.CREATED).body(gestaoMapper.toDto(salva));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<GestaoDTO> atualizar(@PathVariable Integer id, @Valid @RequestBody GestaoDTO gestaoDTO) {
        Gestao gestao = gestaoService.buscarGestaoOuFalhar(id);
        gestao.setNomeGestao(gestaoDTO.nome());
        gestao.setDtInicio(gestaoDTO.dataInicio());
        gestao.setDtFim(gestaoDTO.dataFim());
        Gestao atualizada = gestaoService.atualizarGestao(gestao);
        return ResponseEntity.ok(gestaoMapper.toDto(atualizada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        gestaoService.excluirGestao(id);
        return ResponseEntity.noContent().build();
    }
}