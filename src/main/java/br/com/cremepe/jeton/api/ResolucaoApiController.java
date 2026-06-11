// src/main/java/br/com/cremepe/jeton/api/ResolucaoApiController.java

package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.dto.ResolucaoDTO;
import br.com.cremepe.jeton.mapper.ResolucaoMapper;
import br.com.cremepe.jeton.service.ResolucaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resolucoes")
@PreAuthorize("isAuthenticated()")
public class ResolucaoApiController {

    @Autowired
    private ResolucaoService resolucaoService;

    @Autowired
    private ResolucaoMapper resolucaoMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('R') or hasAuthority('S')")
    public ResponseEntity<List<ResolucaoDTO>> listarTodas() {
        return ResponseEntity.ok(resolucaoService.listarTodos().stream()
                .map(resolucaoMapper::toDto)
                .toList());
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('R') or hasAuthority('S')")
    public ResponseEntity<Page<ResolucaoDTO>> listarPaginado(
            @RequestParam(required = false, defaultValue = "") String termo,
            @RequestParam(required = false, defaultValue = "") String situacao,
            @PageableDefault(size = 10, sort = "ano") Pageable pageable) {
        Page<Resolucao> page = resolucaoService.listarComPaginacaoEPesquisa(termo, situacao,
                pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().get().findFirst().get().getProperty(),
                pageable.getSort().get().findFirst().get().isAscending() ? "asc" : "desc");
        return ResponseEntity.ok(page.map(resolucaoMapper::toDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('R') or hasAuthority('S')")
    public ResponseEntity<ResolucaoDTO> buscarPorId(@PathVariable Integer id) {
        Resolucao resolucao = resolucaoService.buscarOuFalhar(id);
        return ResponseEntity.ok(resolucaoMapper.toDto(resolucao));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<ResolucaoDTO> criar(@Valid @RequestBody ResolucaoDTO dto) {
        Resolucao resolucao = new Resolucao();
        resolucao.setNumero(dto.numero());
        resolucao.setAno(dto.ano());
        resolucao.setDtInicioVigencia(dto.dtInicioVigencia());
        resolucao.setDtFimVigencia(dto.dtFimVigencia());
        resolucao.setEmenta(dto.ementa());
        resolucao.setLinkPublicado(dto.linkPublicado());
        resolucao.setPontosPorJeton(dto.pontosPorJeton());
        resolucao.setMaxJetonsDia(dto.maxJetonsDia());
        resolucao.setMaxJetonsPeriodo(dto.maxJetonsPeriodo());
        resolucao.setMaxJetonsMes(dto.maxJetonsMes());
        resolucao.setValorJeton(dto.valorJeton());
        resolucao.setInRevogado(dto.inRevogado() != null ? dto.inRevogado() : "N");
        Resolucao salva = resolucaoService.criar(resolucao);
        return ResponseEntity.status(HttpStatus.CREATED).body(resolucaoMapper.toDto(salva));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<ResolucaoDTO> atualizar(@PathVariable Integer id, @Valid @RequestBody ResolucaoDTO dto) {
        Resolucao resolucao = resolucaoService.buscarOuFalhar(id);
        resolucao.setNumero(dto.numero());
        resolucao.setAno(dto.ano());
        resolucao.setDtInicioVigencia(dto.dtInicioVigencia());
        resolucao.setDtFimVigencia(dto.dtFimVigencia());
        resolucao.setEmenta(dto.ementa());
        resolucao.setLinkPublicado(dto.linkPublicado());
        resolucao.setPontosPorJeton(dto.pontosPorJeton());
        resolucao.setMaxJetonsDia(dto.maxJetonsDia());
        resolucao.setMaxJetonsPeriodo(dto.maxJetonsPeriodo());
        resolucao.setMaxJetonsMes(dto.maxJetonsMes());
        resolucao.setValorJeton(dto.valorJeton());
        resolucao.setInRevogado(dto.inRevogado());
        Resolucao atualizada = resolucaoService.atualizar(resolucao);
        return ResponseEntity.ok(resolucaoMapper.toDto(atualizada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        resolucaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}