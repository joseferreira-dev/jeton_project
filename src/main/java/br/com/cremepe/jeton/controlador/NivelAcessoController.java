package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.NivelAcesso;
import br.com.cremepe.jeton.servico.NivelAcessoService; 
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/niveis-acesso")
public class NivelAcessoController {

    @Autowired private NivelAcessoService nivelAcessoService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", nivelAcessoService.listarTodos());
        return "nivelacesso/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("nivelAcesso", new NivelAcesso());
        return "nivelacesso/formulario";
    }

    // CORREÇÃO AQUI: Alterado de Integer id para String id
    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") String id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("nivelAcesso", nivelAcessoService.buscarPorId(id).orElse(new NivelAcesso()));
        return "nivelacesso/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("nivelAcesso") NivelAcesso nivelAcesso, RedirectAttributes redirectAttributes) {
        try {
            nivelAcessoService.salvar(nivelAcesso);
            redirectAttributes.addFlashAttribute("sucesso", "Nível de Acesso salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Nível de Acesso.");
        }
        return "redirect:/niveis-acesso";
    }

    // CORREÇÃO AQUI: Alterado de Integer id para String id
    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            nivelAcessoService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Nível de Acesso removido!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover Nível de Acesso.");
        }
        return "redirect:/niveis-acesso";
    }
}