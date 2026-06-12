package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.AtividadeConselhal;
import br.com.cremepe.jeton.dto.AtividadeConselhalDTO;
import br.com.cremepe.jeton.mapper.AtividadeMapper;
import br.com.cremepe.jeton.mapper.ConselheiroMapper;
import br.com.cremepe.jeton.mapper.PortariaMapper;
import br.com.cremepe.jeton.mapper.RegrasMapper;
import br.com.cremepe.jeton.mapper.ResolucaoMapper;
import br.com.cremepe.jeton.service.AtividadeConselhalService;
import br.com.cremepe.jeton.service.ConselheiroService;
import br.com.cremepe.jeton.service.GestaoService;
import br.com.cremepe.jeton.service.RegrasService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/atividades")
@PreAuthorize("isAuthenticated()")
public class AtividadeApiController {

    private final AtividadeConselhalService atividadeService;
    private final ConselheiroService conselheiroService;
    private final GestaoService gestaoService;
    private final RegrasService regrasService;
    private final AtividadeMapper atividadeMapper;
    private final ConselheiroMapper conselheiroMapper;
    private final PortariaMapper portariaMapper;
    private final ResolucaoMapper resolucaoMapper;
    private final RegrasMapper regrasMapper;

    AtividadeApiController(AtividadeConselhalService atividadeService, ConselheiroService conselheiroService,
            GestaoService gestaoService, RegrasService regrasService, ConselheiroMapper conselheiroMapper,
            ResolucaoMapper resolucaoMapper, PortariaMapper portariaMapper, RegrasMapper regrasMapper) {
        this.atividadeService = atividadeService;
        this.conselheiroService = conselheiroService;
        this.gestaoService = gestaoService;
        this.regrasService = regrasService;
        this.atividadeMapper = null;
        this.conselheiroMapper = conselheiroMapper;
        this.resolucaoMapper = resolucaoMapper;
        this.portariaMapper = portariaMapper;
        this.regrasMapper = regrasMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('A') or hasAuthority('S')")
    public ResponseEntity<List<AtividadeConselhalDTO>> listarTodas() {
        List<AtividadeConselhal> atividades = atividadeService.listarTodas();
        List<AtividadeConselhalDTO> dtos = atividades.stream()
                .map(atividadeMapper::toFullDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('A') or hasAuthority('S')")
    public ResponseEntity<Page<AtividadeConselhalDTO>> listarPaginado(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false) String turno,
            @RequestParam(required = false) String comprovanteFiltro,
            @RequestParam(required = false) LocalDateTime dataInicio,
            @RequestParam(required = false) LocalDateTime dataFim,
            @PageableDefault(size = 10, sort = "dataHoraAtividade") Pageable pageable) {

        LocalDate dataInicioDate = dataInicio != null ? dataInicio.toLocalDate() : null;
        LocalDate dataFimDate = dataFim != null ? dataFim.toLocalDate() : null;

        Page<AtividadeConselhal> page = atividadeService.listarComPaginacaoEPesquisa(
                termo, situacao, turno, comprovanteFiltro, dataInicioDate, dataFimDate,
                pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().get().findFirst().get().getProperty(),
                pageable.getSort().get().findFirst().get().isAscending() ? "asc" : "desc");

        return ResponseEntity.ok(page.map(atividadeMapper::toFullDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('A') or hasAuthority('S') or hasAuthority('C')")
    public ResponseEntity<AtividadeConselhalDTO> buscarPorId(@PathVariable Integer id) {
        AtividadeConselhal atividade = atividadeService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada"));
        return ResponseEntity.ok(atividadeMapper.toFullDto(atividade));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('A') or hasAuthority('S')")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        atividadeService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}