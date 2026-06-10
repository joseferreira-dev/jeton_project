package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.dto.RegraDTO;
import br.com.cremepe.jeton.service.ConselheiroService;
import br.com.cremepe.jeton.service.RegrasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/atividades")
@PreAuthorize("isAuthenticated()")
public class AtividadeApiController {

    @Autowired
    private RegrasService regrasService;

    @Autowired
    private ConselheiroService conselheiroService;

    @GetMapping("/filtros-regras")
    public Map<String, Object> getFiltrosRegras(
            @RequestParam(required = false) Integer resolucaoId,
            @RequestParam(required = false) Integer portariaId) {

        Map<String, Object> response = new HashMap<>();

        if (resolucaoId != null && portariaId == null) {
            response.put("portariasCompativeis", regrasService.listarPortariasCompativeis(resolucaoId).stream()
                    .map(p -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", p.getIdPortaria());
                        map.put("nome", "Portaria " + p.getNumero() + "/" + p.getAno());
                        return map;
                    })
                    .collect(Collectors.toList()));
        }
        if (portariaId != null && resolucaoId == null) {
            response.put("resolucoesCompativeis", regrasService.listarResolucoesCompativeis(portariaId).stream()
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", r.getIdResolucao());
                        map.put("nome", "Resolução " + r.getNumero() + "/" + r.getAno());
                        return map;
                    })
                    .collect(Collectors.toList()));
        }

        List<Regras> regras = regrasService.listarRegrasExatas(resolucaoId, portariaId);
        response.put("regras", regras.stream()
                .map(this::converterParaRegraDTO)
                .collect(Collectors.toList()));
        return response;
    }

    @GetMapping("/conselheiros-por-gestao")
    public List<Map<String, Object>> getConselheirosPorGestao(@RequestParam Integer gestaoId) {
        return conselheiroService.listarPorGestao(gestaoId).stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getIdPessoa());
                    map.put("nome", c.getPessoa().getNome());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/regras-por-data")
    public Map<String, Object> getRegrasENormativasPorData(@RequestParam String data) {
        Map<String, Object> response = new HashMap<>();
        try {
            String dataFormatada = data.contains("T") ? data.split("T")[0] : data;
            LocalDate dataAtividade = LocalDate.parse(dataFormatada);

            Optional<Resolucao> optResolucao = regrasService.buscarResolucaoPorData(dataAtividade);
            Optional<Portaria> optPortaria = regrasService.buscarPortariaPorData(dataAtividade);

            Integer idResolucao = optResolucao.map(Resolucao::getIdResolucao).orElse(null);
            Integer idPortaria = optPortaria.map(Portaria::getIdPortaria).orElse(null);

            response.put("idResolucao", idResolucao);
            response.put("nomeResolucao", optResolucao.map(this::formatarResolucao).orElse("Nenhuma encontrada"));
            response.put("idPortaria", idPortaria);
            response.put("nomePortaria", optPortaria.map(this::formatarPortaria).orElse("Nenhuma (Apenas Resolução)"));

            if (idResolucao != null) {
                List<Regras> listaRegras = regrasService.listarRegrasPorNormativasInclusiveRevogadas(idResolucao,
                        idPortaria);
                response.put("regras", listaRegras.stream()
                        .sorted(Comparator.comparing(Regras::getNomeRegra))
                        .map(this::converterParaRegraDTO)
                        .collect(Collectors.toList()));
            } else {
                response.put("regras", Collections.emptyList());
            }
        } catch (Exception e) {
            response.put("erro", "Formato de data inválido ou erro interno ao processar.");
            response.put("regras", Collections.emptyList());
        }
        return response;
    }

    private RegraDTO converterParaRegraDTO(Regras regra) {
        return new RegraDTO(regra.getIdRegra(), regra.getNomeRegra(), regra.getDescricao(), regra.getPontos());
    }

    private String formatarPortaria(Portaria p) {
        return "Portaria " + p.getNumero() + "/" + p.getAno();
    }

    private String formatarResolucao(Resolucao r) {
        return "Resolução " + r.getNumero() + "/" + r.getAno();
    }
}