package br.com.cremepe.jeton.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.service.EmailService;
import br.com.cremepe.jeton.service.UsuarioService;

@Controller
public class RecuperacaoSenhaController {

    private final UsuarioService usuarioService;
    private final EmailService emailService;

    public RecuperacaoSenhaController(UsuarioService usuarioService, EmailService emailService) {
        this.usuarioService = usuarioService;
        this.emailService = emailService;
    }

    @GetMapping("/recuperar-senha")
    public String exibirFormulario() {
        return "recuperacao-senha"; // template Thymeleaf
    }

    @PostMapping("/recuperar-senha")
    public String processarRecuperacao(@RequestParam("cpfOuEmail") String cpfOuEmail,
            RedirectAttributes ra) {
        try {
            String senha = usuarioService.gerarSenhaProvisoria(cpfOuEmail);

            Usuario usuario = usuarioService.buscarPorCpfOuEmail(cpfOuEmail)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            emailService.enviarSenhaProvisoria(
                    usuario.getPessoa().getEmail(),
                    usuario.getPessoa().getNome(),
                    senha);

            ra.addFlashAttribute("sucesso", "Nova senha enviada para seu e-mail.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/recuperar-senha";
    }
}