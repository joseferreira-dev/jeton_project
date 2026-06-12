package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.service.GestaoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gestoes")
@PreAuthorize("hasAuthority('G') or hasAuthority('S')")
public class GestaoController {

    private final GestaoService gestaoService;

    GestaoController(GestaoService gestaoService) {
        this.gestaoService = gestaoService;
    }

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

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("gestao", new Gestao());
        model.addAttribute("urlAcao", "/gestoes/salvar");
        return "gestao/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        Gestao gestao = gestaoService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Gestão não encontrada para o ID: " + id));
        model.addAttribute("gestao", gestao);
        model.addAttribute("urlAcao", "/gestoes/atualizar");
        return "gestao/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("gestao") Gestao gestao,
            RedirectAttributes ra) {
        gestaoService.criarGestao(gestao);
        ra.addFlashAttribute("sucesso", "Gestão criada com sucesso!");
        return "redirect:/gestoes";
    }

    @PostMapping("/atualizar")
    public String atualizar(@Valid @ModelAttribute("gestao") Gestao gestao,
            RedirectAttributes ra) {
        gestaoService.atualizarGestao(gestao);
        ra.addFlashAttribute("sucesso", "Gestão atualizada com sucesso!");
        return "redirect:/gestoes";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes ra) {
        gestaoService.excluirGestao(id);
        ra.addFlashAttribute("sucesso", "Gestão removida com sucesso!");
        return "redirect:/gestoes";
    }

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }
}