package br.com.cremepe.jeton.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/erro")
public class ErroController {

    @GetMapping
    public String erro(@RequestParam(value = "tipo", required = false) String tipo, Model model) {
        String titulo = "Erro";
        String mensagem = "Ocorreu um erro inesperado.";
        if ("sistema_bloqueado".equals(tipo)) {
            titulo = "Sistema Bloqueado";
            mensagem = "O sistema encontra-se bloqueado para manutenção. Apenas administradores podem realizar ações neste momento.";
        }
        model.addAttribute("titulo", titulo);
        model.addAttribute("mensagem", mensagem);
        return "erro";
    }
}