package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.ViewAtividadeConselhal;
import br.com.cremepe.jeton.dto.RelatorioAtividadeConselhalAgrupadoDTO;
import br.com.cremepe.jeton.repositorio.ViewAtividadeConselhalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    @Autowired
    private ViewAtividadeConselhalRepository viewRepository;

    @Transactional(readOnly = true)
    public List<RelatorioAtividadeConselhalAgrupadoDTO> gerarRelatorioAgrupado(Integer idGestao, String nomeConselheiro) {
        List<ViewAtividadeConselhal> dadosRaw = viewRepository.findByIdGestao(idGestao);

        // 1. Filtrar pelo conselheiro selecionado, se houver
        if (nomeConselheiro != null && !nomeConselheiro.isEmpty()) {
            dadosRaw = dadosRaw.stream()
                    .filter(at -> nomeConselheiro.equals(at.getNome()))
                    .collect(Collectors.toList());
        }

        if (dadosRaw.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Extrair todas as regras ÚNICAS da base de dados selecionada.
        // Isto é VITAL para garantir que as colunas da tabela fiquem perfeitamente alinhadas para todos os médicos.
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
            
            // Inicializa TODAS as regras mapeadas com ZERO (Garante a integridade do layout da tabela)
            todasRegras.forEach(regra -> dto.getRegras().put(regra, 0));

            // Soma (Merge) os valores reais caso o médico tenha feito a mesma atividade várias vezes
            atividades.forEach(at -> {
                Integer qtd = (at.getQtdAtividade() != null) ? at.getQtdAtividade() : 0;
                dto.getRegras().merge(at.getNomeRegra(), qtd, Integer::sum);
            });

            relatorio.add(dto);
        });

        // Ordena o relatório por ordem alfabética do conselheiro
        relatorio.sort(Comparator.comparing(RelatorioAtividadeConselhalAgrupadoDTO::getConselheiro));

        return relatorio;
    }
}