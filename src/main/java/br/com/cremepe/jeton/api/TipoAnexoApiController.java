package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.TipoAnexo;
import br.com.cremepe.jeton.dto.TipoAnexoDTO;
import br.com.cremepe.jeton.mapper.TipoAnexoMapper;
import br.com.cremepe.jeton.service.TipoAnexoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-anexo")
@PreAuthorize("isAuthenticated()")
public class TipoAnexoApiController {

    @Autowired
    private TipoAnexoService tipoAnexoService;

    @Autowired
    private TipoAnexoMapper tipoAnexoMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('T') or hasAuthority('S')")
    public ResponseEntity<List<TipoAnexoDTO>> listarTodos() {
        return ResponseEntity.ok(tipoAnexoService.listarTodos().stream()
                .map(tipoAnexoMapper::toDto)
                .toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('T') or hasAuthority('S')")
    public ResponseEntity<TipoAnexoDTO> buscarPorId(@PathVariable Integer id) {
        TipoAnexo tipo = tipoAnexoService.buscarOuFalhar(id);
        return ResponseEntity.ok(tipoAnexoMapper.toDto(tipo));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<TipoAnexoDTO> criar(@Valid @RequestBody TipoAnexoDTO dto) {
        TipoAnexo tipo = new TipoAnexo();
        tipo.setNome(dto.nome());
        tipo.setExigePublicacao(dto.exigePublicacao() ? "S" : "N");
        TipoAnexo salvo = tipoAnexoService.criar(tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoAnexoMapper.toDto(salvo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<TipoAnexoDTO> atualizar(@PathVariable Integer id, @Valid @RequestBody TipoAnexoDTO dto) {
        TipoAnexo tipo = tipoAnexoService.buscarOuFalhar(id);
        tipo.setNome(dto.nome());
        tipo.setExigePublicacao(dto.exigePublicacao() ? "S" : "N");
        TipoAnexo atualizado = tipoAnexoService.atualizar(tipo);
        return ResponseEntity.ok(tipoAnexoMapper.toDto(atualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        tipoAnexoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}