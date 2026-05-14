package br.com.cremepe.jeton.controlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.RelatorioService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

    @Autowired private RelatorioService relatorioService;
    
    // Injeções para popular os combos (selects) de filtro na tela de relatório
    @Autowired private ConselheiroService conselheiroService;
    @Autowired private GestaoService gestaoService;

    // Método para acessar a tela inicial do relatório (apenas os filtros vazios)
    @GetMapping("/atividade-agrupada")
    public String acessarTelaRelatorio(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        
        carregarFiltros(model);
        return "relatorio/atividade_agrupada"; // HTML do relatório
    }

    // Método que recebe os dados do formulário de filtro e devolve a lista gerada
    @GetMapping("/atividade-agrupada/gerar")
    public String gerarRelatorio(
            @RequestParam(value = "idGestao", required = false) Integer idGestao,
            Model model, HttpSession session) {
            
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";

        // Chama o método exato que existe no seu RelatorioService
        var dadosRelatorio = relatorioService.gerarRelatorioAgrupado(idGestao);
        
        model.addAttribute("listaRelatorio", dadosRelatorio);
        carregarFiltros(model);
        
        return "relatorio/atividade_agrupada"; 
    }

    private void carregarFiltros(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
    }
}