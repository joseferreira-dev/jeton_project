package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.ViewAtividadeConselhal;
import br.com.cremepe.jeton.dto.RelatorioAtividadeConselhalAgrupadoDTO;
import br.com.cremepe.jeton.repositorio.ViewAtividadeConselhalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    @Autowired
    private ViewAtividadeConselhalRepository viewRepository;

    @Transactional(readOnly = true)
    public List<RelatorioAtividadeConselhalAgrupadoDTO> gerarRelatorioAgrupado(
            Integer idGestao, Integer idConselheiro, Integer idRegra, LocalDate dataInicio, LocalDate dataFim) {

        List<ViewAtividadeConselhal> dadosRaw = viewRepository.findByIdGestao(idGestao);

        // 1. Aplicação Dinâmica dos Novos Filtros
        if (idConselheiro != null) {
            dadosRaw = dadosRaw.stream()
                    .filter(at -> idConselheiro.equals(at.getIdPessoa()))
                    .collect(Collectors.toList());
        }

        if (idRegra != null) {
            dadosRaw = dadosRaw.stream()
                    .filter(at -> idRegra.equals(at.getIdRegra()))
                    .collect(Collectors.toList());
        }

        if (dataInicio != null) {
            dadosRaw = dadosRaw.stream()
                    .filter(at -> at.getDataHoraAtividade() != null
                            && !at.getDataHoraAtividade().toLocalDate().isBefore(dataInicio))
                    .collect(Collectors.toList());
        }

        if (dataFim != null) {
            dadosRaw = dadosRaw.stream()
                    .filter(at -> at.getDataHoraAtividade() != null
                            && !at.getDataHoraAtividade().toLocalDate().isAfter(dataFim))
                    .collect(Collectors.toList());
        }

        if (dadosRaw.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Extrair regras ÚNICAS da base filtrada para manter as colunas alinhadas
        Set<String> todasRegras = dadosRaw.stream()
                .map(ViewAtividadeConselhal::getNomeRegra)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 3. Agrupar os dados por Conselheiro
        Map<String, List<ViewAtividadeConselhal>> agrupadoPorNome = dadosRaw.stream()
                .collect(Collectors.groupingBy(ViewAtividadeConselhal::getNome));

        List<RelatorioAtividadeConselhalAgrupadoDTO> relatorio = new ArrayList<>();

        agrupadoPorNome.forEach((nome, atividades) -> {
            RelatorioAtividadeConselhalAgrupadoDTO dto = new RelatorioAtividadeConselhalAgrupadoDTO();
            dto.setConselheiro(nome);
            dto.setGestao(atividades.get(0).getNomeGestao());

            // Inicializa TODAS as regras mapeadas com ZERO
            todasRegras.forEach(regra -> dto.getRegras().put(regra, 0));

            // Soma (Merge) os valores reais
            atividades.forEach(at -> {
                Integer qtd = (at.getQtdAtividade() != null) ? at.getQtdAtividade() : 0;
                dto.getRegras().merge(at.getNomeRegra(), qtd, Integer::sum);
            });

            relatorio.add(dto);
        });

        // Ordenar alfabeticamente
        relatorio.sort(Comparator.comparing(RelatorioAtividadeConselhalAgrupadoDTO::getConselheiro));

        return relatorio;
    }
}