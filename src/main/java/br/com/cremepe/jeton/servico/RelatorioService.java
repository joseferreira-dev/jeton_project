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

        // 1. CHAMADA DINÂMICA AO BANCO: Delega a filtragem opcional de datas e
        // conselheiro ao SQL
        List<ViewAtividadeConselhal> dadosRaw = viewRepository.buscarParaRelatorioDynamic(
                idGestao, idConselheiro, dataInicio, dataFim);

        // Se o banco não retornar nada, interrompe para evitar NullPointerException
        if (dadosRaw == null || dadosRaw.isEmpty()) {
            return new ArrayList<>();
        }

        // Filtro residual caso o idRegra seja informado (mantido por compatibilidade)
        if (idRegra != null) {
            dadosRaw = dadosRaw.stream()
                    .filter(at -> idRegra.equals(at.getIdRegra()))
                    .collect(Collectors.toList());
        }

        // 2. Extrair regras ÚNICAS da base filtrada para manter as colunas alinhadas na
        // tabela
        Set<String> todasRegras = dadosRaw.stream()
                .map(ViewAtividadeConselhal::getNomeRegra)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 3. Agrupar os dados consolidados por Nome do Conselheiro
        Map<String, List<ViewAtividadeConselhal>> agrupadoPorNome = dadosRaw.stream()
                .filter(at -> at.getNome() != null)
                .collect(Collectors.groupingBy(ViewAtividadeConselhal::getNome));

        List<RelatorioAtividadeConselhalAgrupadoDTO> relatorio = new ArrayList<>();

        agrupadoPorNome.forEach((nome, atividades) -> {
            RelatorioAtividadeConselhalAgrupadoDTO dto = new RelatorioAtividadeConselhalAgrupadoDTO();
            dto.setConselheiro(nome);
            dto.setGestao(atividades.get(0).getNomeGestao());

            // Inicializa TODAS as regras mapeadas da tabela com ZERO
            todasRegras.forEach(regra -> dto.getRegras().put(regra, 0));

            // Soma (Merge) a quantidade de atividades reais executadas
            atividades.forEach(at -> {
                Integer qtd = (at.getQtdAtividade() != null) ? at.getQtdAtividade() : 0;
                if (at.getNomeRegra() != null) {
                    dto.getRegras().merge(at.getNomeRegra(), qtd, Integer::sum);
                }
            });

            // Calcula o Score Total somando (Quantidade * Pontos da Regra)
            int totalPontos = atividades.stream()
                    .mapToInt(at -> {
                        int qtd = (at.getQtdAtividade() != null) ? at.getQtdAtividade() : 0;
                        int pontos = (at.getPontos() != null) ? at.getPontos() : 0;
                        return qtd * pontos;
                    }).sum();

            dto.setTotalPontos(totalPontos);
            relatorio.add(dto);
        });

        // Ordena o relatório final por ordem alfabética do conselheiro
        relatorio.sort(Comparator.comparing(RelatorioAtividadeConselhalAgrupadoDTO::getConselheiro));

        return relatorio;
    }
}