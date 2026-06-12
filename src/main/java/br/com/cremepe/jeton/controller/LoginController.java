package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.ViewUserLogin;
import br.com.cremepe.jeton.service.DashboardService;
import br.com.cremepe.jeton.service.ParametrosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final ParametrosService parametrosService;
    private final DashboardService dashboardService;

    LoginController(ParametrosService parametrosService, DashboardService dashboardService) {
        this.parametrosService = parametrosService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/login")
    public String telaLogin(HttpSession session, Model model) {
        // Se já estiver autenticado, redireciona para a página apropriada
        ViewUserLogin usuarioLogado = (ViewUserLogin) session.getAttribute("usuarioLogado");
        if (usuarioLogado != null) {
            if ("C".equals(usuarioLogado.getInTipoPessoa())) {
                return "redirect:/conselheiro/dashboard";
            } else {
                return "redirect:/index";
            }
        }

        // Recupera mensagem de erro da sessão (setada pelo failure handler) e remove
        String erro = (String) session.getAttribute("erro");
        if (erro != null) {
            model.addAttribute("erro", erro);
            session.removeAttribute("erro");
        }

        return "login";
    }

    @GetMapping("/index")
    public String index(HttpSession session, Model model) {
        ViewUserLogin usuarioLogado = (ViewUserLogin) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/login";
        }

        if ("C".equals(usuarioLogado.getInTipoPessoa())) {
            return "redirect:/conselheiro/dashboard";
        }

        model.addAttribute("totalPendentes", dashboardService.getTotalAtividadesPendentes());
        model.addAttribute("totalConselheiros", dashboardService.getTotalConselheirosAtivos());
        model.addAttribute("totalAtividadesMes", dashboardService.getTotalAtividadesDoMes());
        model.addAttribute("totalComprovantes", dashboardService.getTotalComprovantes());
        model.addAttribute("atividadesRecentes", dashboardService.getUltimasAtividades(5));
        model.addAttribute("sistemaBloqueado", parametrosService.isSistemaBloqueado());

        return "index";
    }
}