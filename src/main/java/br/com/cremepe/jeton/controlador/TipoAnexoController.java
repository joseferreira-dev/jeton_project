package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.TipoAnexo;
import br.com.cremepe.jeton.servico.TipoAnexoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tipos-anexo")
public class TipoAnexoController {

    @Autowired private TipoAnexoService tipoAnexoService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", tipoAnexoService.listarTodos());
        return "tipoanexo/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("tipoAnexo", new TipoAnexo());
        return "tipoanexo/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("tipoAnexo", tipoAnexoService.buscarPorId(id).orElse(new TipoAnexo()));
        return "tipoanexo/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("tipoAnexo") TipoAnexo tipoAnexo, RedirectAttributes redirectAttributes) {
        try {
            tipoAnexoService.salvar(tipoAnexo);
            redirectAttributes.addFlashAttribute("sucesso", "Tipo de Anexo salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Tipo de Anexo.");
        }
        return "redirect:/tipos-anexo";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            tipoAnexoService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Tipo de Anexo removido!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover Tipo de Anexo.");
        }
        return "redirect:/tipos-anexo";
    }
}