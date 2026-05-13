package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.servico.ConselheiroService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/conselheiros")
public class ConselheiroController {

    @Autowired private ConselheiroService conselheiroService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", conselheiroService.listarTodos());
        return "conselheiro/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("conselheiro", new Conselheiro());
        return "conselheiro/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("conselheiro", conselheiroService.buscarPorId(id).orElse(new Conselheiro()));
        return "conselheiro/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("conselheiro") Conselheiro conselheiro, RedirectAttributes redirectAttributes) {
        try {
            conselheiroService.salvar(conselheiro);
            redirectAttributes.addFlashAttribute("sucesso", "Conselheiro salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Conselheiro.");
        }
        return "redirect:/conselheiros";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            conselheiroService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Conselheiro removido!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover Conselheiro.");
        }
        return "redirect:/conselheiros";
    }
}