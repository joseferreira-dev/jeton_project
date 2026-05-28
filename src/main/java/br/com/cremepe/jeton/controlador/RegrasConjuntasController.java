package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.RegrasConjuntas;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.servico.RegrasConjuntasService;
import br.com.cremepe.jeton.servico.RegrasService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/regras-conjuntas")
public class RegrasConjuntasController {

    @Autowired
    private RegrasConjuntasService regrasConjuntasService;
    @Autowired
    private RegrasService regrasService;

    // =========================================================================
    // LISTAGEM
    // =========================================================================
    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "tipoLimite", required = false, defaultValue = "") String tipoLimite,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "nomeRegra") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "asc") String dir,
            Model model, HttpSession session) {

        if (naoAutenticado(session))
            return "redirect:/login";

        Page<RegrasConjuntas> pagina = regrasConjuntasService.listarComPaginacaoEPesquisa(
                termo, tipoLimite, page, size, sort, dir);
        model.addAttribute("paginaRegrasConjuntas", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("tipoLimite", tipoLimite);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "regrasconjuntas/lista";
    }

    // =========================================================================
    // FORMULÁRIOS
    // =========================================================================
    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("regrasConjuntas", new RegrasConjuntas());
        carregarListasApoio(model);
        return "regrasconjuntas/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        RegrasConjuntas regra = regrasConjuntasService.buscarOuFalhar(id);
        model.addAttribute("regrasConjuntas", regra);
        carregarListasApoio(model);
        return "regrasconjuntas/formulario";
    }

    private void carregarListasApoio(Model model) {
        model.addAttribute("listaRegras", regrasService.listarTodos());
    }

    // =========================================================================
    // SALVAR
    // =========================================================================
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("regrasConjuntas") RegrasConjuntas regrasConjuntas,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Integer idUsuarioLogado = getIdUsuarioLogado(session);
            regrasConjuntasService.salvar(regrasConjuntas, idUsuarioLogado);
            ra.addFlashAttribute("sucesso", "Regras Conjuntas salvas com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao salvar as Regras Conjuntas.");
        }
        return "redirect:/regras-conjuntas";
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
            regrasConjuntasService.excluir(id, idUsuarioLogado);
            ra.addFlashAttribute("sucesso", "Regra Conjunta removida com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao remover a Regra Conjunta.");
        }
        return "redirect:/regras-conjuntas";
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