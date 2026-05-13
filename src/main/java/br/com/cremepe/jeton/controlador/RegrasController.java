package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.servico.RegrasService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/regras")
public class RegrasController {

    @Autowired private RegrasService regrasService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", regrasService.listarTodos());
        return "regras/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("regra", new Regras());
        return "regras/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("regra", regrasService.buscarPorId(id).orElse(new Regras()));
        return "regras/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("regra") Regras regra, RedirectAttributes redirectAttributes) {
        try {
            regrasService.salvar(regra);
            redirectAttributes.addFlashAttribute("sucesso", "Regra salva com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Regra.");
        }
        return "redirect:/regras";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            regrasService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Regra removida!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover Regra.");
        }
        return "redirect:/regras";
    }
}