package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.servico.ResolucaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/resolucoes")
public class ResolucaoController {

    @Autowired private ResolucaoService resolucaoService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", resolucaoService.listarTodos());
        return "resolucao/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("resolucao", new Resolucao());
        return "resolucao/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("resolucao", resolucaoService.buscarPorId(id).orElse(new Resolucao()));
        return "resolucao/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("resolucao") Resolucao resolucao, RedirectAttributes redirectAttributes) {
        try {
            resolucaoService.salvar(resolucao);
            redirectAttributes.addFlashAttribute("sucesso", "Resolução salva com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Resolução.");
        }
        return "redirect:/resolucoes";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            resolucaoService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Resolução removida!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover Resolução.");
        }
        return "redirect:/resolucoes";
    }
}