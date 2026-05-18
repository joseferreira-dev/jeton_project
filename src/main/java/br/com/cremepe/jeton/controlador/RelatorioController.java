package br.com.cremepe.jeton.controlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.RegrasService;
import br.com.cremepe.jeton.servico.RelatorioService;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;

@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

    @Autowired private RelatorioService relatorioService;
    @Autowired private ConselheiroService conselheiroService;
    @Autowired private GestaoService gestaoService;
    @Autowired private RegrasService regrasService; // NOVO SERVIÇO INJETADO

    @GetMapping("/atividades")
    public String acessarTelaRelatorio(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        carregarFiltros(model);
        return "relatorio/atividade_agrupada"; 
    }

    @GetMapping("/atividades/gerar")
    public String gerarRelatorio(
            @RequestParam(value = "idGestao", required = false) Integer idGestao,
            @RequestParam(value = "idConselheiro", required = false) Integer idConselheiro,
            @RequestParam(value = "idRegra", required = false) Integer idRegra,
            @RequestParam(value = "dataInicio", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dataInicio,
            @RequestParam(value = "dataFim", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dataFim,
            Model model, HttpSession session) {
            
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";

        // Chama a nova lógica complexa
        var dadosRelatorio = relatorioService.gerarRelatorioAgrupado(idGestao, idConselheiro, idRegra, dataInicio, dataFim);
        model.addAttribute("listaRelatorio", dadosRelatorio);
        
        if (!dadosRelatorio.isEmpty()) {
            model.addAttribute("colunasRegras", dadosRelatorio.get(0).getRegras().keySet());
        }
        
        carregarFiltros(model);
        return "relatorio/atividade_agrupada"; 
    }

    private void carregarFiltros(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestao", gestaoService.listarTodos()); 
        model.addAttribute("listaRegras", regrasService.listarTodos()); 
    }
}