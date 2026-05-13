package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.servico.UsuarioService; // Mudamos a importação para o Service
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class LoginController {

    // INJEÇÃO CORRETA: O Controlador chama o Serviço (Regra de Negócio)
    @Autowired
    private UsuarioService usuarioService;

    // Equivalente a carregar a tela de login.xhtml
    @GetMapping("/login")
    public String telaLogin() {
        return "login"; 
    }

    // Equivalente ao método logar() do antigo AutenticarBean.java
    @PostMapping("/autenticar")
    public String autenticar(@RequestParam("cpf") String cpf,
                             @RequestParam("senha") String senha,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        
        // CORREÇÃO: Chama o método autenticar que já existe no seu UsuarioService!
        Optional<ViewUserLogin> usuarioOpt = usuarioService.autenticar(cpf, senha);

        if (usuarioOpt.isPresent()) {
            ViewUserLogin usuarioLogado = usuarioOpt.get();
            // Guarda o usuário na sessão
            session.setAttribute("usuarioLogado", usuarioLogado);
            
            // Redireciona para a página inicial
            return "redirect:/index"; 
        } else {
            // Devolve mensagem de erro
            redirectAttributes.addFlashAttribute("erro", "CPF ou Senha inválidos!");
            return "redirect:/login";
        }
    }

    // Equivalente ao método sair() do antigo AutenticarBean.java
    @GetMapping("/sair")
    public String sair(HttpSession session) {
        session.invalidate(); // Limpa a sessão
        return "redirect:/login";
    }

    // Página inicial após o login
    @GetMapping("/index")
    public String index(HttpSession session, Model model) {
        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/login";
        }
        return "index"; 
    }
}