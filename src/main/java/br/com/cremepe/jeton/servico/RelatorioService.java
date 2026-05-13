package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.ViewAtividadeConselhal;
import br.com.cremepe.jeton.dto.RelatorioAtividadeConselhalAgrupadoDTO;
import br.com.cremepe.jeton.repositorio.ViewAtividadeConselhalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    @Autowired
    private ViewAtividadeConselhalRepository viewRepository;

    /**
     * Gera o relatório Pivot agrupado por Conselheiro e Regras.
     * Esta lógica substitui o processamento manual que o Java antigo fazia nos DAOs de relatório.
     */
    @Transactional(readOnly = true)
    public List<RelatorioAtividadeConselhalAgrupadoDTO> gerarRelatorioAgrupado(Integer idGestao) {
        List<ViewAtividadeConselhal> dadosRaw = viewRepository.findByIdGestao(idGestao);

        // Agrupamos os dados por Conselheiro usando Java Streams (Moderno)
        Map<String, List<ViewAtividadeConselhal>> agrupadoPorNome = dadosRaw.stream()
                .collect(Collectors.groupingBy(ViewAtividadeConselhal::getNome));

        List<RelatorioAtividadeConselhalAgrupadoDTO> relatorio = new ArrayList<>();

        agrupadoPorNome.forEach((nome, atividades) -> {
            RelatorioAtividadeConselhalAgrupadoDTO dto = new RelatorioAtividadeConselhalAgrupadoDTO();
            dto.setConselheiro(nome);
            dto.setGestao(atividades.get(0).getNomeGestao());
            
            // Preenche o Map de regras dinâmicas (Pivot)
            atividades.forEach(at -> {
                dto.adicionarRegra(at.getNomeRegra(), at.getQtdAtividade());
            });
            
            relatorio.add(dto);
        });

        return relatorio;
    }
}