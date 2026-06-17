package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.TipoAnexo;
import br.com.cremepe.jeton.service.TipoAnexoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tipos-anexo")
@PreAuthorize("hasAuthority('T') or hasAuthority('S')")
public class TipoAnexoController {

    private final TipoAnexoService tipoAnexoService;

    TipoAnexoController(TipoAnexoService tipoAnexoService) {
        this.tipoAnexoService = tipoAnexoService;
    }

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("lista", tipoAnexoService.listarTodos());
        return "tipoanexo/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("tipoAnexo", new TipoAnexo());
        return "tipoanexo/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        TipoAnexo tipoAnexo = tipoAnexoService.buscarOuFalhar(id);
        model.addAttribute("tipoAnexo", tipoAnexo);
        return "tipoanexo/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("tipoAnexo") TipoAnexo tipoAnexo,
            HttpSession session,
            RedirectAttributes ra) {
        boolean isNovo = tipoAnexo.getIdTipo() == null;

        if (isNovo) {
            tipoAnexoService.criar(tipoAnexo);
            ra.addFlashAttribute("sucesso", "Tipo de Anexo criado com sucesso!");
        } else {
            tipoAnexoService.atualizar(tipoAnexo);
            ra.addFlashAttribute("sucesso", "Tipo de Anexo atualizado com sucesso!");
        }
        return "redirect:/tipos-anexo";
    }

    @PostMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes ra) {
        tipoAnexoService.excluir(id);
        ra.addFlashAttribute("sucesso", "Tipo de Anexo removido com sucesso!");
        return "redirect:/tipos-anexo";
    }

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }
}