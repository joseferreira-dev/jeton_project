package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.TipoAnexo;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
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

    @Autowired
    private TipoAnexoService tipoAnexoService;

    // =========================================================================
    // LISTAGEM
    // =========================================================================
    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("lista", tipoAnexoService.listarTodos());
        return "tipoanexo/lista";
    }

    // =========================================================================
    // FORMULÁRIOS
    // =========================================================================
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

    // =========================================================================
    // SALVAR (CRIAR / ATUALIZAR)
    // =========================================================================
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("tipoAnexo") TipoAnexo tipoAnexo,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Integer idUsuarioLogado = getIdUsuarioLogado(session);
            tipoAnexoService.salvar(tipoAnexo, idUsuarioLogado);
            ra.addFlashAttribute("sucesso", "Tipo de Anexo salvo com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao salvar tipo de anexo.");
        }
        return "redirect:/tipos-anexo";
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================
    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            Integer idUsuarioLogado = getIdUsuarioLogado(session);
            tipoAnexoService.excluir(id, idUsuarioLogado);
            ra.addFlashAttribute("sucesso", "Tipo de Anexo removido com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao remover tipo de anexo.");
        }
        return "redirect:/tipos-anexo";
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================
    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }

    private Integer getIdUsuarioLogado(HttpSession session) {
        ViewUserLogin usuario = (ViewUserLogin) session.getAttribute("usuarioLogado");
        return usuario != null ? usuario.getIdPessoa() : null;
    }
}