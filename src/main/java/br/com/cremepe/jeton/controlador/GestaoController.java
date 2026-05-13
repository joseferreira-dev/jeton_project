package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.servico.GestaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gestoes")
public class GestaoController {

    @Autowired private GestaoService gestaoService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", gestaoService.listarTodos());
        return "gestao/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("gestao", new Gestao());
        return "gestao/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("gestao", gestaoService.buscarPorId(id).orElse(new Gestao()));
        return "gestao/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("gestao") Gestao gestao, RedirectAttributes redirectAttributes) {
        try {
            gestaoService.salvar(gestao);
            redirectAttributes.addFlashAttribute("sucesso", "Gestão salva com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Gestão.");
        }
        return "redirect:/gestoes";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            gestaoService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Gestão removida!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover Gestão.");
        }
        return "redirect:/gestoes";
    }
}