package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.service.ResolucaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/resolucoes")
@PreAuthorize("hasAuthority('R') or hasAuthority('S')")
public class ResolucaoController {

    @Autowired
    private ResolucaoService resolucaoService;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "ano") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "desc") String dir,
            Model model, HttpSession session) {

        if (naoAutenticado(session))
            return "redirect:/login";

        Page<Resolucao> pagina = resolucaoService.listarComPaginacaoEPesquisa(termo, situacao, page, size, sort, dir);
        model.addAttribute("paginaResolucoes", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "resolucao/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("resolucao", new Resolucao());
        return "resolucao/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        Resolucao resolucao = resolucaoService.buscarOuFalhar(id);
        model.addAttribute("resolucao", resolucao);
        return "resolucao/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("resolucao") Resolucao resolucao,
            HttpSession session,
            RedirectAttributes ra) {
        boolean isNovo = resolucao.getIdResolucao() == null;

        if (isNovo) {
            resolucaoService.criar(resolucao);
            ra.addFlashAttribute("sucesso", "Resolução criada com sucesso!");
        } else {
            resolucaoService.atualizar(resolucao);
            ra.addFlashAttribute("sucesso", "Resolução atualizada com sucesso!");
        }
        return "redirect:/resolucoes";
    }

    @GetMapping("/revogar/{id}")
    public String revogar(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        resolucaoService.revogar(id);
        ra.addFlashAttribute("sucesso", "Resolução revogada com sucesso!");
        return "redirect:/resolucoes";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        resolucaoService.restaurar(id);
        ra.addFlashAttribute("sucesso", "Resolução restaurada (em vigor) com sucesso!");
        return "redirect:/resolucoes";
    }

    @GetMapping("/deletar/{id}")
    public String excluir(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        resolucaoService.excluir(id);
        ra.addFlashAttribute("sucesso", "Resolução excluída definitivamente.");
        return "redirect:/resolucoes";
    }

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }
}