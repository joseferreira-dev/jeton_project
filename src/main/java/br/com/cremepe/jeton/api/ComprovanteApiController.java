package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.Comprovante;
import br.com.cremepe.jeton.dto.ComprovanteDTO;
import br.com.cremepe.jeton.mapper.ComprovanteMapper;
import br.com.cremepe.jeton.service.ComprovanteService;
import br.com.cremepe.jeton.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/comprovantes")
@PreAuthorize("isAuthenticated()")
public class ComprovanteApiController {

    private final ComprovanteService comprovanteService;
    private final FileStorageService fileStorageService;
    private final ComprovanteMapper comprovanteMapper;

    ComprovanteApiController(ComprovanteService comprovanteService, FileStorageService fileStorageService,
            ComprovanteMapper comprovanteMapper) {
        this.comprovanteService = comprovanteService;
        this.fileStorageService = fileStorageService;
        this.comprovanteMapper = comprovanteMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('C') or hasAuthority('S')")
    public ResponseEntity<List<ComprovanteDTO>> listarTodos() {
        return ResponseEntity.ok(comprovanteService.listarTodos().stream()
                .map(comprovanteMapper::toDto)
                .toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('C') or hasAuthority('S')")
    public ResponseEntity<ComprovanteDTO> buscarPorId(@PathVariable Integer id) {
        Comprovante comp = comprovanteService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Comprovante não encontrado"));
        return ResponseEntity.ok(comprovanteMapper.toDto(comp));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('C') or hasAuthority('S')")
    public ResponseEntity<Resource> downloadComprovante(@PathVariable Integer id) {
        Comprovante comp = comprovanteService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Comprovante não encontrado"));
        Resource resource = fileStorageService.carregarArquivo(comp.getNomeArquivo(), comp.getAno(), comp.getMes());
        String contentType = comp.getContentType() != null ? comp.getContentType() : "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + comp.getNomeComprovante() + "\"")
                .body(resource);
    }
}