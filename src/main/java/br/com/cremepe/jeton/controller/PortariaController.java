package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.service.PortariaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/portarias")
@PreAuthorize("hasAuthority('R') or hasAuthority('S')")
public class PortariaController {

    private final PortariaService portariaService;

    PortariaController(PortariaService portariaService) {
        this.portariaService = portariaService;
    }

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

        Page<Portaria> pagina = portariaService.listarComPaginacaoEPesquisa(termo, situacao, page, size, sort, dir);
        model.addAttribute("paginaPortarias", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "portaria/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("portaria", new Portaria());
        return "portaria/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        Portaria portaria = portariaService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Portaria não encontrada"));
        model.addAttribute("portaria", portaria);
        return "portaria/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("portaria") Portaria portaria,
            HttpSession session,
            RedirectAttributes ra) {
        boolean isNovo = portaria.getIdPortaria() == null;

        if (isNovo) {
            portariaService.criar(portaria);
            ra.addFlashAttribute("sucesso", "Portaria criada com sucesso!");
        } else {
            portariaService.atualizar(portaria);
            ra.addFlashAttribute("sucesso", "Portaria atualizada com sucesso!");
        }
        return "redirect:/portarias";
    }

    @GetMapping("/revogar/{id}")
    public String revogar(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        portariaService.revogar(id);
        ra.addFlashAttribute("sucesso", "Portaria revogada com sucesso!");
        return "redirect:/portarias";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        portariaService.restaurar(id);
        ra.addFlashAttribute("sucesso", "Portaria restaurada (em vigor) com sucesso!");
        return "redirect:/portarias";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        portariaService.excluir(id);
        ra.addFlashAttribute("sucesso", "Portaria excluída definitivamente.");
        return "redirect:/portarias";
    }

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }
}