package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.servico.GestaoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gestoes")
public class GestaoController {

    @Autowired
    private GestaoService gestaoService;

    // =========================================================================
    // LISTAGEM
    // =========================================================================
    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = Gestao.SORT_DT_INICIO) String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "desc") String dir,
            Model model, HttpSession session) {

        if (naoAutenticado(session))
            return "redirect:/login";

        Page<Gestao> pagina = gestaoService.listarComPaginacaoEPesquisa(termo, page, size, sort, dir);

        model.addAttribute("paginaGestoes", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);

        return "gestao/lista";
    }

    // =========================================================================
    // FORMULÁRIOS (NOVO / EDIÇÃO)
    // =========================================================================
    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("gestao", new Gestao());
        return "gestao/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        Gestao gestao = gestaoService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Gestão não encontrada para o ID: " + id));
        model.addAttribute("gestao", gestao);
        return "gestao/formulario";
    }

    // =========================================================================
    // SALVAR (CRIAR / ATUALIZAR)
    // =========================================================================
    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("gestao") Gestao gestao,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Integer idUsuarioLogado = getIdUsuarioLogado(session);
            gestaoService.salvar(gestao, idUsuarioLogado);
            ra.addFlashAttribute("sucesso", "Gestão salva com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/gestoes";
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================
    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Integer idUsuarioLogado = getIdUsuarioLogado(session);
            gestaoService.excluir(id, idUsuarioLogado);
            ra.addFlashAttribute("sucesso", "Gestão removida com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro",
                    "Não foi possível remover. Verifique se existem conselheiros ou atividades vinculadas a esta gestão.");
        }
        return "redirect:/gestoes";
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