package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Parametros;
import br.com.cremepe.jeton.servico.ParametrosService; 
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/parametros")
public class ParametrosController {

    @Autowired private ParametrosService parametrosService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", parametrosService.listarTodos());
        return "parametros/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("parametro", new Parametros());
        return "parametros/formulario";
    }

    // CORREÇÃO AQUI: Alterado de Integer id para String id
    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") String id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("parametro", parametrosService.buscarPorId(id).orElse(new Parametros()));
        return "parametros/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("parametro") Parametros parametro, RedirectAttributes redirectAttributes) {
        try {
            parametrosService.salvar(parametro);
            redirectAttributes.addFlashAttribute("sucesso", "Parâmetros salvos com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Parâmetros.");
        }
        return "redirect:/parametros";
    }
}