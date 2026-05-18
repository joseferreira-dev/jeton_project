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
    @Autowired private ConselheiroService conselheiroService;
    @Autowired private GestaoService gestaoService;

    // Rota alterada para coincidir com o menu
    @GetMapping("/atividades")
    public String acessarTelaRelatorio(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        
        carregarFiltros(model);
        return "relatorio/atividade_agrupada"; 
    }

    // Rota do processamento ajustada
    @GetMapping("/atividades/gerar")
    public String gerarRelatorio(
            @RequestParam(value = "idGestao", required = false) Integer idGestao,
            @RequestParam(value = "idConselheiro", required = false) Integer idConselheiro,
            Model model, HttpSession session) {
            
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";

        // Traduz o ID do Conselheiro para o Nome (para facilitar o filtro de String na View)
        String nomeFiltro = null;
        if (idConselheiro != null) {
            nomeFiltro = conselheiroService.buscarPorId(idConselheiro)
                                           .map(c -> c.getPessoa().getNome())
                                           .orElse(null);
        }

        var dadosRelatorio = relatorioService.gerarRelatorioAgrupado(idGestao, nomeFiltro);
        model.addAttribute("listaRelatorio", dadosRelatorio);
        
        // Passar os cabeçalhos de forma SEGURA para evitar o erro SpEL na View HTML
        if (!dadosRelatorio.isEmpty()) {
            model.addAttribute("colunasRegras", dadosRelatorio.get(0).getRegras().keySet());
        }
        
        carregarFiltros(model);
        return "relatorio/atividade_agrupada"; 
    }

    private void carregarFiltros(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestao", gestaoService.listarTodos()); 
    }
}