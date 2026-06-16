package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.ViewAtividadeConselhal;
import br.com.cremepe.jeton.dto.AtividadeAgrupadaRelatorioDTO;
import br.com.cremepe.jeton.repository.ViewAtividadeConselhalRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    private static final Logger log = LoggerFactory.getLogger(RelatorioService.class);

    private final ViewAtividadeConselhalRepository viewRepository;
    private final LogJetonService logJetonService;

    public RelatorioService(ViewAtividadeConselhalRepository viewRepository,
            LogJetonService logJetonService) {
        this.viewRepository = viewRepository;
        this.logJetonService = logJetonService;
    }

    @Transactional(readOnly = true)
    public List<AtividadeAgrupadaRelatorioDTO> gerarRelatorioAgrupado(
            Integer idGestao, Integer idConselheiro, Integer idRegra,
            LocalDate dataInicio, LocalDate dataFim) {

        log.debug("Gerando relatório agrupado: gestão={}, conselheiro={}, dataInicio={}, dataFim={}",
                idGestao, idConselheiro, dataInicio, dataFim);

        LocalDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = (dataFim != null) ? dataFim.atTime(LocalTime.MAX) : null;

        List<ViewAtividadeConselhal> dadosRaw = viewRepository.findForReport(
                idGestao, idConselheiro, inicio, fim);

        if (dadosRaw == null || dadosRaw.isEmpty()) {
            log.debug("Nenhum dado encontrado para os filtros informados.");
            logJetonService.logRelatorioGerado(idGestao, idConselheiro, idRegra, dataInicio, dataFim, 0);
            return Collections.emptyList();
        }

        // Filtro adicional por regra (se fornecido)
        if (idRegra != null) {
            dadosRaw = dadosRaw.stream()
                    .filter(at -> idRegra.equals(at.getIdRegra()))
                    .collect(Collectors.toList());
        }

        // Conjunto de todas as regras presentes (para criar colunas)
        Set<String> todasRegras = dadosRaw.stream()
                .map(ViewAtividadeConselhal::getNomeRegra)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Agrupa por nome do conselheiro
        Map<String, List<ViewAtividadeConselhal>> agrupadoPorNome = dadosRaw.stream()
                .filter(at -> at.getNome() != null)
                .collect(Collectors.groupingBy(ViewAtividadeConselhal::getNome));

        List<AtividadeAgrupadaRelatorioDTO> relatorio = new ArrayList<>();

        agrupadoPorNome.forEach((nome, atividades) -> {
            AtividadeAgrupadaRelatorioDTO dto = new AtividadeAgrupadaRelatorioDTO();
            dto.setConselheiro(nome);
            dto.setGestao(atividades.get(0).getNomeGestao());

            todasRegras.forEach(regra -> dto.getRegras().put(regra, 0));

            atividades.forEach(at -> {
                Integer qtd = at.getQtdAtividade() != null ? at.getQtdAtividade() : 0;
                if (at.getNomeRegra() != null) {
                    dto.getRegras().merge(at.getNomeRegra(), qtd, Integer::sum);
                }
            });

            int totalPontos = atividades.stream()
                    .mapToInt(at -> {
                        int qtd = at.getQtdAtividade() != null ? at.getQtdAtividade() : 0;
                        int pontos = at.getPontos() != null ? at.getPontos() : 0;
                        return qtd * pontos;
                    }).sum();
            dto.setTotalPontos(totalPontos);

            relatorio.add(dto);
        });

        relatorio.sort(Comparator.comparing(AtividadeAgrupadaRelatorioDTO::getConselheiro));
        log.info("Relatório gerado com {} registros", relatorio.size());

        logJetonService.logRelatorioGerado(idGestao, idConselheiro, idRegra, dataInicio, dataFim, relatorio.size());

        return relatorio;
    }
}