package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.RelatorioService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

    @Autowired
    private RelatorioService relatorioService;

    @Autowired
    private ConselheiroService conselheiroService;

    @Autowired
    private GestaoService gestaoService;

    @GetMapping("/atividades")
    public String acessarTelaRelatorio(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";

        model.addAttribute("listaRelatorio", java.util.Collections.emptyList());
        carregarFiltros(model);
        return "relatorio/atividade_agrupada";
    }

    @GetMapping("/atividades/gerar")
    public String gerarRelatorio(
            @RequestParam(value = "idGestao", required = false) Integer idGestao,
            @RequestParam(value = "idConselheiro", required = false) Integer idConselheiro,
            @RequestParam(value = "dataInicio", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dataInicio,
            @RequestParam(value = "dataFim", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dataFim,
            Model model, HttpSession session) {

        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";

        // Validação básica: Gestão é obrigatória para o relatório
        if (idGestao == null) {
            model.addAttribute("erro", "Por favor, selecione uma gestão.");
            carregarFiltros(model);
            return "relatorio/atividade_agrupada";
        }

        // Executa o relatório passando os filtros (que podem ir nulos caso não
        // preenchidos)
        var dadosRelatorio = relatorioService.gerarRelatorioAgrupado(idGestao, idConselheiro, null, dataInicio,
                dataFim);
        model.addAttribute("listaRelatorio", dadosRelatorio);

        if (!dadosRelatorio.isEmpty()) {
            model.addAttribute("colunasRegras", dadosRelatorio.get(0).getRegras().keySet());
        }

        // Devolve os parâmetros de busca para o formulário no HTML manter o estado
        // visual
        model.addAttribute("idGestaoSelecionada", idGestao);
        model.addAttribute("idConselheiroSelecionado", idConselheiro);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);

        carregarFiltros(model);
        return "relatorio/atividade_agrupada";
    }

    private void carregarFiltros(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
    }
}