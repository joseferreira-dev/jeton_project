package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.servico.PortariaService; // Seu serviço existente no novo projeto
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/portarias")
public class PortariaController {

    @Autowired private PortariaService portariaService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", portariaService.listarTodos());
        return "portaria/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("portaria", new Portaria());
        return "portaria/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("portaria", portariaService.buscarPorId(id).orElse(new Portaria()));
        return "portaria/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("portaria") Portaria portaria, RedirectAttributes redirectAttributes) {
        try {
            portariaService.salvar(portaria);
            redirectAttributes.addFlashAttribute("sucesso", "Portaria salva com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Portaria: " + e.getMessage());
        }
        return "redirect:/portarias";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            portariaService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Portaria removida com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover Portaria.");
        }
        return "redirect:/portarias";
    }
}