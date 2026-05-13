package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Comprovante;
import br.com.cremepe.jeton.servico.ComprovanteService;
import br.com.cremepe.jeton.servico.TipoAnexoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/comprovantes")
public class ComprovanteController {

    @Autowired private ComprovanteService comprovanteService;
    @Autowired private TipoAnexoService tipoAnexoService; // Para popular o <select>

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", comprovanteService.listarTodos());
        return "comprovante/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("comprovante", new Comprovante());
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
        return "comprovante/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("comprovante", comprovanteService.buscarPorId(id).orElse(new Comprovante()));
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
        return "comprovante/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("comprovante") Comprovante comprovante, RedirectAttributes redirectAttributes) {
        try {
            // Futuramente aqui você acopla a chamada para salvar o arquivo físico/FTP
            comprovanteService.salvar(comprovante);
            redirectAttributes.addFlashAttribute("sucesso", "Comprovante salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Comprovante: " + e.getMessage());
        }
        return "redirect:/comprovantes";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            comprovanteService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Comprovante removido!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover Comprovante.");
        }
        return "redirect:/comprovantes";
    }
}