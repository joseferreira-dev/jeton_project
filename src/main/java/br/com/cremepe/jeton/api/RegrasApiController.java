package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.dto.PortariaDTO;
import br.com.cremepe.jeton.dto.RegraDTO;
import br.com.cremepe.jeton.dto.ResolucaoDTO;
import br.com.cremepe.jeton.mapper.PortariaMapper;
import br.com.cremepe.jeton.mapper.RegrasMapper;
import br.com.cremepe.jeton.mapper.ResolucaoMapper;
import br.com.cremepe.jeton.service.RegrasService;
import br.com.cremepe.jeton.service.ResolucaoService;
import br.com.cremepe.jeton.service.PortariaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/regras")
@PreAuthorize("isAuthenticated()")
public class RegrasApiController {

    private final RegrasService regrasService;
    private final ResolucaoService resolucaoService;
    private final PortariaService portariaService;
    private final RegrasMapper regrasMapper;
    private final PortariaMapper portariaMapper;
    private final ResolucaoMapper resolucaoMapper;

    RegrasApiController(RegrasService regrasService, PortariaService portariaService, RegrasMapper regrasMapper,
            ResolucaoService resolucaoService, PortariaMapper portariaMapper,
            ResolucaoMapper resolucaoMapper) {
        this.regrasService = regrasService;
        this.resolucaoService = resolucaoService;
        this.portariaService = portariaService;
        this.regrasMapper = regrasMapper;
        this.portariaMapper = portariaMapper;
        this.resolucaoMapper = resolucaoMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('R') or hasAuthority('S')")
    public ResponseEntity<List<RegraDTO>> listarTodas() {
        return ResponseEntity.ok(regrasService.listarTodos().stream()
                .map(regrasMapper::toDto)
                .toList());
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('R') or hasAuthority('S')")
    public ResponseEntity<Page<RegraDTO>> listarPaginado(
            @RequestParam(required = false, defaultValue = "") String termo,
            @RequestParam(required = false, defaultValue = "") String situacao,
            @RequestParam(required = false, defaultValue = "") String judicante,
            @PageableDefault(size = 10, sort = "nomeRegra") Pageable pageable) {
        Page<Regras> page = regrasService.listarComPaginacaoEPesquisa(termo, situacao, judicante,
                pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().get().findFirst().get().getProperty(),
                pageable.getSort().get().findFirst().get().isAscending() ? "asc" : "desc");
        return ResponseEntity.ok(page.map(regrasMapper::toDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('R') or hasAuthority('S')")
    public ResponseEntity<RegraDTO> buscarPorId(@PathVariable Integer id) {
        Regras regra = regrasService.buscarOuFalhar(id);
        return ResponseEntity.ok(regrasMapper.toDto(regra));
    }

    @GetMapping("/resolucao/{resolucaoId}")
    @PreAuthorize("hasAuthority('R') or hasAuthority('S')")
    public ResponseEntity<List<RegraDTO>> listarPorResolucao(@PathVariable Integer resolucaoId) {
        List<RegraDTO> dtos = regrasService.listarRegrasPorResolucao(resolucaoId).stream()
                .map(regrasMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<RegraDTO> criar(@Valid @RequestBody RegraDTO dto) {
        Regras regra = new Regras();

        if (dto.resolucaoId() == null) {
            throw new RuntimeException("A resolução é obrigatória. Selecione uma resolução válida.");
        }
        Resolucao resolucao = resolucaoService.buscarOuFalhar(dto.resolucaoId());
        regra.setResolucao(resolucao);

        if (dto.portariaId() != null) {
            Portaria portaria = portariaService.buscarOuFalhar(dto.portariaId());
            regra.setPortaria(portaria);
        }

        regra.setNomeRegra(dto.nome());
        regra.setDescricao(dto.descricao());
        regra.setPontos(dto.pontos());
        regra.setInRevogado(dto.inRevogado() != null ? dto.inRevogado() : "N");
        regra.setPontosLimitesTurno(dto.pontosLimitesTurno() != null ? dto.pontosLimitesTurno() : 0);
        regra.setInJudicante(dto.inJudicante() != null ? dto.inJudicante() : "N");

        Regras salva = regrasService.criar(regra);
        return ResponseEntity.status(HttpStatus.CREATED).body(regrasMapper.toDto(salva));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<RegraDTO> atualizar(@PathVariable Integer id, @Valid @RequestBody RegraDTO dto) {
        Regras regra = regrasService.buscarOuFalhar(id);

        if (dto.resolucaoId() == null) {
            throw new RuntimeException("A resolução é obrigatória.");
        }
        Resolucao resolucao = resolucaoService.buscarOuFalhar(dto.resolucaoId());
        regra.setResolucao(resolucao);

        if (dto.portariaId() != null) {
            Portaria portaria = portariaService.buscarOuFalhar(dto.portariaId());
            regra.setPortaria(portaria);
        } else {
            regra.setPortaria(null);
        }

        regra.setNomeRegra(dto.nome());
        regra.setDescricao(dto.descricao());
        regra.setPontos(dto.pontos());
        regra.setInRevogado(dto.inRevogado() != null ? dto.inRevogado() : regra.getInRevogado());
        regra.setPontosLimitesTurno(
                dto.pontosLimitesTurno() != null ? dto.pontosLimitesTurno() : regra.getPontosLimitesTurno());
        regra.setInJudicante(dto.inJudicante() != null ? dto.inJudicante() : regra.getInJudicante());

        Regras atualizada = regrasService.atualizar(regra);
        return ResponseEntity.ok(regrasMapper.toDto(atualizada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('S')")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        regrasService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtros-regras")
    public Map<String, Object> getFiltrosRegras(
            @RequestParam(required = false) Integer resolucaoId,
            @RequestParam(required = false) Integer portariaId) {

        Map<String, Object> response = new HashMap<>();

        if (resolucaoId != null && portariaId == null) {
            List<PortariaDTO> portarias = regrasService.listarPortariasCompativeis(resolucaoId).stream()
                    .map(portariaMapper::toDto)
                    .collect(Collectors.toList());
            response.put("portariasCompativeis", portarias);
        }
        if (portariaId != null && resolucaoId == null) {
            List<ResolucaoDTO> resolucoes = regrasService.listarResolucoesCompativeis(portariaId).stream()
                    .map(resolucaoMapper::toDto)
                    .collect(Collectors.toList());
            response.put("resolucoesCompativeis", resolucoes);
        }

        List<Regras> regras = regrasService.listarRegrasExatas(resolucaoId, portariaId);
        response.put("regras", regras.stream()
                .map(regrasMapper::toDto)
                .collect(Collectors.toList()));
        return response;
    }

    @GetMapping("/regras-por-data")
    public Map<String, Object> getRegrasENormativasPorData(@RequestParam String data) {
        String dataFormatada = data.contains("T") ? data.split("T")[0] : data;
        LocalDate dataAtividade = LocalDate.parse(dataFormatada);

        Optional<Resolucao> optResolucao = regrasService.buscarResolucaoPorData(dataAtividade);
        Optional<Portaria> optPortaria = regrasService.buscarPortariaPorData(dataAtividade);

        Integer idResolucao = optResolucao.map(Resolucao::getIdResolucao).orElse(null);
        Integer idPortaria = optPortaria.map(Portaria::getIdPortaria).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("idResolucao", idResolucao);
        response.put("nomeResolucao", optResolucao
                .map(r -> "Resolução " + r.getNumero() + "/" + r.getAno())
                .orElse("Nenhuma encontrada"));
        response.put("idPortaria", idPortaria);
        response.put("nomePortaria", optPortaria
                .map(p -> "Portaria " + p.getNumero() + "/" + p.getAno())
                .orElse("Nenhuma (Apenas Resolução)"));

        if (idResolucao != null) {
            List<Regras> listaRegras = regrasService.listarRegrasPorNormativasInclusiveRevogadas(idResolucao,
                    idPortaria);
            response.put("regras", listaRegras.stream()
                    .sorted(Comparator.comparing(Regras::getNomeRegra))
                    .map(regrasMapper::toDto)
                    .collect(Collectors.toList()));
        } else {
            response.put("regras", Collections.emptyList());
        }
        return response;
    }
}