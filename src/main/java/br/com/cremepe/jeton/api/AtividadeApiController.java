package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.AtividadeConselhal;
import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.dto.AtividadeConselhalDTO;
import br.com.cremepe.jeton.dto.PortariaDTO;
import br.com.cremepe.jeton.dto.ResolucaoDTO;
import br.com.cremepe.jeton.mapper.AtividadeMapper;
import br.com.cremepe.jeton.mapper.ConselheiroMapper;
import br.com.cremepe.jeton.mapper.PortariaMapper;
import br.com.cremepe.jeton.mapper.RegrasMapper;
import br.com.cremepe.jeton.mapper.ResolucaoMapper;
import br.com.cremepe.jeton.service.AtividadeConselhalService;
import br.com.cremepe.jeton.service.ConselheiroService;
import br.com.cremepe.jeton.service.GestaoService;
import br.com.cremepe.jeton.service.RegrasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/atividades")
@PreAuthorize("isAuthenticated()")
public class AtividadeApiController {

    @Autowired
    private AtividadeConselhalService atividadeService;

    @Autowired
    private ConselheiroService conselheiroService;

    @Autowired
    private GestaoService gestaoService;

    @Autowired
    private RegrasService regrasService;

    @Autowired
    private AtividadeMapper atividadeMapper;

    @Autowired
    private ConselheiroMapper conselheiroMapper;

    @Autowired
    private PortariaMapper portariaMapper;

    @Autowired
    private ResolucaoMapper resolucaoMapper;

    @Autowired
    private RegrasMapper regrasMapper;

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