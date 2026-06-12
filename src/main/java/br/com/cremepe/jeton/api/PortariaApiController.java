package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.dto.PortariaDTO;
import br.com.cremepe.jeton.mapper.PortariaMapper;
import br.com.cremepe.jeton.service.PortariaService;
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
@RequestMapping("/api/portarias")
@PreAuthorize("isAuthenticated()")
public class PortariaApiController {

    private final PortariaService portariaService;
    private final PortariaMapper portariaMapper;

    PortariaApiController(PortariaService portariaService, PortariaMapper portariaMapper) {
        this.portariaService = portariaService;
        this.portariaMapper = portariaMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('R') or hasAuthority('S')")
    public ResponseEntity<List<PortariaDTO>> listarTodas() {
        List<PortariaDTO> dtos = portariaService.listarTodos().stream()
                .map(portariaMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('R') or hasAuthority('S')")
    public ResponseEntity<Page<PortariaDTO>> listarPaginado(
            @RequestParam(required = false, defaultValue = "") String termo,
            @RequestParam(required = false, defaultValue = "") String situacao,
            @PageableDefault(size = 10, sort = "ano") Pageable pageable) {
        Page<Portaria> page = portariaService.listarComPaginacaoEPesquisa(termo, situacao,
                pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().get().findFirst().get().getProperty(),
                pageable.getSort().get().findFirst().get().isAscending() ? "asc" : "desc");
        Page<PortariaDTO> dtoPage = page.map(portariaMapper::toDto);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('R') or hasAuthority('S')")
    public ResponseEntity<PortariaDTO> buscarPorId(@PathVariable Integer id) {
        Portaria portaria = portariaService.buscarOuFalhar(id);
        return ResponseEntity.ok(portariaMapper.toDto(portaria));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<PortariaDTO> criar(@Valid @RequestBody PortariaDTO dto) {
        Portaria portaria = new Portaria();
        portaria.setNumero(dto.numero());
        portaria.setAno(dto.ano());
        portaria.setDtInicioVigencia(dto.dtInicioVigencia());
        portaria.setDtFimVigencia(dto.dtFimVigencia());
        portaria.setLinkPublicado(dto.linkPublicado());
        Portaria salva = portariaService.criar(portaria);
        return ResponseEntity.status(HttpStatus.CREATED).body(portariaMapper.toDto(salva));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<PortariaDTO> atualizar(@PathVariable Integer id, @Valid @RequestBody PortariaDTO dto) {
        Portaria portaria = portariaService.buscarOuFalhar(id);
        portaria.setNumero(dto.numero());
        portaria.setAno(dto.ano());
        portaria.setDtInicioVigencia(dto.dtInicioVigencia());
        portaria.setDtFimVigencia(dto.dtFimVigencia());
        portaria.setLinkPublicado(dto.linkPublicado());
        portaria.setInRevogado(dto.inRevogado());
        Portaria atualizada = portariaService.atualizar(portaria);
        return ResponseEntity.ok(portariaMapper.toDto(atualizada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        portariaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}