package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repositorio.ComprovanteRepository;
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.servico.UsuarioService;
import jakarta.servlet.http.HttpSession;
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

    @Autowired
    private UsuarioService usuarioService;

    // Injeção dos repositórios para alimentar o Dashboard
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

        Optional<ViewUserLogin> usuarioOpt = usuarioService.autenticar(cpf, senha);

        if (usuarioOpt.isPresent()) {
            ViewUserLogin usuarioLogado = usuarioOpt.get();
            session.setAttribute("usuarioLogado", usuarioLogado);
            return "redirect:/index";
        } else {
            redirectAttributes.addFlashAttribute("erro", "CPF ou Senha inválidos!");
            return "redirect:/login";
        }
    }

    @GetMapping("/sair")
    public String sair(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // Página inicial (Dashboard) totalmente funcional e dinâmica
    @GetMapping("/index")
    public String index(HttpSession session, Model model) {
        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/login";
        }

        // 1. Total de Atividades Pendentes de Validação (Situação 'P')
        long totalPendentes = atividadeRepository.countByInSituacao("P");

        // 2. Total de Conselheiros Ativos (Situação 'A')
        long totalConselheiros = conselheiroRepository.countByInSituacao("A");

        // 3. Atividades Registadas no Mês Atual
        LocalDate hoje = LocalDate.now();
        long totalAtividadesMes = atividadeRepository.countAtividadesDoMes(hoje.getMonthValue(), hoje.getYear());

        // 4. Total de Comprovantes Salvos
        long totalComprovantes = comprovanteRepository.count();

        // 5. Atividades Recentes
        List<AtividadeConselhal> atividadesRecentes = atividadeRepository.findTop5ByOrderByDataHoraRegistroDesc();

        // Envia as métricas para a tela index.html
        model.addAttribute("totalPendentes", totalPendentes);
        model.addAttribute("totalConselheiros", totalConselheiros);
        model.addAttribute("totalAtividadesMes", totalAtividadesMes);
        model.addAttribute("totalComprovantes", totalComprovantes);
        model.addAttribute("atividadesRecentes", atividadesRecentes);

        return "index";
    }
}