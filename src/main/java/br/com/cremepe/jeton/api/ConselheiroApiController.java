package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.Pessoa;
import br.com.cremepe.jeton.dto.ConselheiroDTO;
import br.com.cremepe.jeton.dto.ConselheiroResponseDTO;
import br.com.cremepe.jeton.mapper.ConselheiroMapper;
import br.com.cremepe.jeton.service.ConselheiroService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conselheiros")
@PreAuthorize("isAuthenticated()")
public class ConselheiroApiController {

    private final ConselheiroService conselheiroService;
    private final ConselheiroMapper conselheiroMapper;

    ConselheiroApiController(ConselheiroService conselheiroService, ConselheiroMapper conselheiroMapper) {
        this.conselheiroService = conselheiroService;
        this.conselheiroMapper = conselheiroMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('G') or hasAuthority('S')")
    public ResponseEntity<List<ConselheiroResponseDTO>> listarTodos() {
        List<ConselheiroResponseDTO> dtos = conselheiroService.listarTodos().stream()
                .map(conselheiroMapper::toResponseDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('G') or hasAuthority('S')")
    public ResponseEntity<Page<ConselheiroResponseDTO>> listarPaginado(
            @RequestParam(required = false, defaultValue = "") String termo,
            @RequestParam(required = false, defaultValue = "") String situacao,
            @PageableDefault(size = 10, sort = "pessoa.nome") Pageable pageable) {
        Page<Conselheiro> page = conselheiroService.listarComPaginacaoEPesquisa(termo, situacao,
                pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().get().findFirst().get().getProperty(),
                pageable.getSort().get().findFirst().get().isAscending() ? "asc" : "desc");
        return ResponseEntity.ok(page.map(conselheiroMapper::toResponseDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('G') or hasAuthority('S')")
    public ResponseEntity<ConselheiroResponseDTO> buscarPorId(@PathVariable Integer id) {
        Conselheiro conselheiro = conselheiroService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado"));
        return ResponseEntity.ok(conselheiroMapper.toResponseDto(conselheiro));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<ConselheiroDTO> criar(@Valid @RequestBody ConselheiroDTO dto) {
        // Cria a entidade Pessoa
        Pessoa pessoa = new Pessoa();
        pessoa.setNome(dto.nome());
        pessoa.setEmail(dto.email());
        pessoa.setCpf(dto.cpf());
        pessoa.setInTipoPessoa(Pessoa.TIPO_CONSELHEIRO);

        // Cria o Conselheiro e associa a Pessoa
        Conselheiro conselheiro = new Conselheiro();
        conselheiro.setPessoa(pessoa);
        conselheiro.setCrm(dto.crm());
        conselheiro.setInSituacao(dto.situacao() != null ? dto.situacao() : Conselheiro.SITUACAO_ATIVO);
        conselheiro.setSenhaAcesso(dto.senha());

        // Delega a criação para o service
        Conselheiro salvo = conselheiroService.criar(conselheiro);
        return ResponseEntity.status(HttpStatus.CREATED).body(conselheiroMapper.toDto(salvo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<ConselheiroDTO> atualizar(@PathVariable Integer id, @Valid @RequestBody ConselheiroDTO dto) {
        Conselheiro existente = conselheiroService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado"));

        // Atualiza os dados da Pessoa associada
        Pessoa pessoa = existente.getPessoa();
        pessoa.setNome(dto.nome());
        pessoa.setEmail(dto.email());
        pessoa.setCpf(dto.cpf());

        // Atualiza dados do Conselheiro
        existente.setCrm(dto.crm());
        existente.setInSituacao(dto.situacao() != null ? dto.situacao() : existente.getInSituacao());

        // O service de atualização já trata a persistência da Pessoa
        Conselheiro atualizado = conselheiroService.atualizar(existente);
        return ResponseEntity.ok(conselheiroMapper.toDto(atualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        conselheiroService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conselheiros-por-gestao")
    public List<ConselheiroDTO> getConselheirosPorGestao(@RequestParam Integer gestaoId) {
        return conselheiroService.listarPorGestao(gestaoId).stream()
                .map(conselheiroMapper::toDto)
                .collect(Collectors.toList());
    }
}