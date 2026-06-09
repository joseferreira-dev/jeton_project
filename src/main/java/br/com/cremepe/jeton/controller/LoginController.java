package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.ViewUserLogin;
import br.com.cremepe.jeton.service.DashboardService;
import br.com.cremepe.jeton.service.LoginService;
import br.com.cremepe.jeton.service.ParametrosService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private static final String ERRO_LOGIN = "CPF ou Senha inválidos!";

    @Autowired
    private LoginService loginService;
    @Autowired
    private ParametrosService parametrosService;
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/login")
    public String telaLogin() {
        return "login";
    }

    @PostMapping("/autenticar")
    public String autenticar(@RequestParam("cpf") String cpf,
            @RequestParam("senha") String senha,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (cpf == null || cpf.isBlank() || senha == null || senha.isBlank()) {
            log.warn("Tentativa de login com CPF ou senha vazios");
            return redirecionaParaLoginComErro(redirectAttributes);
        }

        try {
            ViewUserLogin usuarioLogado = loginService.login(cpf, senha);
            session.setAttribute("usuarioLogado", usuarioLogado);

            if ("C".equals(usuarioLogado.getInTipoPessoa())) {
                return "redirect:/conselheiro";
            } else {
                return "redirect:/index";
            }
        } catch (RuntimeException e) {
            log.warn("Falha de autenticação para CPF: {}", cpf.replaceAll(".(?=.{4})", "*"));
            return redirecionaParaLoginComErro(redirectAttributes);
        }
    }

    private String redirecionaParaLoginComErro(RedirectAttributes ra) {
        ra.addFlashAttribute("erro", ERRO_LOGIN);
        return "redirect:/login";
    }

    @GetMapping("/sair")
    public String sair(HttpSession session) {
        ViewUserLogin usuario = (ViewUserLogin) session.getAttribute("usuarioLogado");
        if (usuario != null) {
            loginService.logout(usuario.getIdPessoa(), usuario.getNome());
        }
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/index")
    public String index(HttpSession session, Model model) {
        ViewUserLogin usuarioLogado = (ViewUserLogin) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/login";
        }

        if ("C".equals(usuarioLogado.getInTipoPessoa())) {
            return "redirect:/conselheiro";
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