package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repositorio.ComprovanteRepository;
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.servico.LogJetonService;
import br.com.cremepe.jeton.servico.UsuarioService;
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
import java.util.Optional;

@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private static final String ERRO_LOGIN = "CPF ou Senha inválidos!";

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private LogJetonService logJetonService;

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

        // Validação simples antes de consultar o banco
        if (cpf == null || cpf.isBlank() || senha == null || senha.isBlank()) {
            log.warn("Tentativa de login com CPF ou senha vazios");
            // Não registra em log_jeton pois não há usuário identificado
            return redirectToLoginWithError(redirectAttributes);
        }

        String cpfMascarado = cpf.replaceAll(".(?=.{4})", "*");
        Optional<ViewUserLogin> usuarioOpt = usuarioService.autenticar(cpf, senha);

        if (usuarioOpt.isPresent()) {
            ViewUserLogin usuarioLogado = usuarioOpt.get();
            session.setAttribute("usuarioLogado", usuarioLogado);
            log.info("Login bem-sucedido: usuário {} (ID {})", usuarioLogado.getNome(), usuarioLogado.getIdPessoa());

            // Log de auditoria para login bem-sucedido
            String textoLog = String.format(
                    "Login bem-sucedido: Usuário ID=%d, Nome='%s', CPF=%s",
                    usuarioLogado.getIdPessoa(), usuarioLogado.getNome(), cpfMascarado);
            logJetonService.registrarLog("login", usuarioLogado.getIdPessoa(), textoLog);

            // Redireciona conselheiros para o portal, outros para o dashboard
            // administrativo
            if ("C".equals(usuarioLogado.getInTipoPessoa())) {
                return "redirect:/conselheiro";
            } else {
                return "redirect:/index";
            }
        } else {
            log.warn("Falha de autenticação para CPF: {}", cpfMascarado);

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
            log.info("Logout do usuário: {} (ID {})", usuario.getNome(), usuario.getIdPessoa());

            // Log de auditoria para logout
            String textoLog = String.format(
                    "Logout realizado: Usuário ID=%d, Nome='%s'",
                    usuario.getIdPessoa(), usuario.getNome());
            logJetonService.registrarLog("login", usuario.getIdPessoa(), textoLog);
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

        // Conselheiros não devem ver o dashboard administrativo
        if ("C".equals(usuarioLogado.getInTipoPessoa())) {
            return "redirect:/conselheiro";
        }

        // Métricas do dashboard (apenas para funcionários e super admin)
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

        return "index";
    }
}