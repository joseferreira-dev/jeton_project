package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repositorio.ComprovanteRepository;
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.servico.LoginService;
import br.com.cremepe.jeton.servico.ParametrosService;
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

import java.time.LocalDate;
import java.util.List;

@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private static final String ERRO_LOGIN = "CPF ou Senha inválidos!";

    @Autowired
    private LoginService loginService;
    @Autowired
    private ParametrosService parametrosService;

    // Dashboard
    @Autowired
    private AtividadeConselhalRepository atividadeRepository;
    @Autowired
    private ConselheiroRepository conselheiroRepository;
    @Autowired
    private ComprovanteRepository comprovanteRepository;

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
            return redirectToLoginWithError(redirectAttributes);
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
            return redirectToLoginWithError(redirectAttributes);
        }
    }

    private String redirectToLoginWithError(RedirectAttributes ra) {
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

        long totalPendentes = atividadeRepository.countByInSituacao("P");
        long totalConselheiros = conselheiroRepository.countByInSituacao("A");
        LocalDate hoje = LocalDate.now();
        long totalAtividadesMes = atividadeRepository.countAtividadesDoMes(hoje.getMonthValue(), hoje.getYear());
        long totalComprovantes = comprovanteRepository.count();
        List<AtividadeConselhal> atividadesRecentes = atividadeRepository.findTop5ByOrderByDataHoraRegistroDesc();

        model.addAttribute("totalPendentes", totalPendentes);
        model.addAttribute("totalConselheiros", totalConselheiros);
        model.addAttribute("totalAtividadesMes", totalAtividadesMes);
        model.addAttribute("totalComprovantes", totalComprovantes);
        model.addAttribute("atividadesRecentes", atividadesRecentes);
        model.addAttribute("sistemaBloqueado", parametrosService.isSistemaBloqueado());

        return "index";
    }
}