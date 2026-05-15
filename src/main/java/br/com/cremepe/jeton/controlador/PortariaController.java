package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.servico.PortariaService; // Seu serviço existente no novo projeto
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/portarias")
public class PortariaController {

    @Autowired private PortariaService portariaService;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "ano") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "desc") String dir,
            Model model, HttpSession session) {
            
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";

        Page<Portaria> paginaPortarias = portariaService.listarComPaginacaoEPesquisa(termo, situacao, page, size, sort, dir);

        model.addAttribute("paginaPortarias", paginaPortarias);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);

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
            portariaService.revogar(id); // Usa o novo método
            redirectAttributes.addFlashAttribute("sucesso", "Portaria revogada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao revogar Portaria.");
        }
        return "redirect:/portarias";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            portariaService.restaurar(id);
            redirectAttributes.addFlashAttribute("sucesso", "Portaria restaurada (Em Vigor) com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao restaurar Portaria.");
        }
        return "redirect:/portarias";
    }

    @GetMapping("/deletar/{id}")
    public String deletar(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            portariaService.excluirFisicamente(id);
            ra.addFlashAttribute("sucesso", "Portaria excluída definitivamente.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao excluir portaria.");
        }
        return "redirect:/portarias";
    }
}