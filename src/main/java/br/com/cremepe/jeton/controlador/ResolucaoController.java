package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.servico.ResolucaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/resolucoes")
public class ResolucaoController {

    @Autowired private ResolucaoService resolucaoService;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "ano") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "desc") String dir,
            Model model, HttpSession session) {
            
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";

        Page<Resolucao> paginaResolucoes = resolucaoService.listarComPaginacaoEPesquisa(termo, situacao, page, size, sort, dir);

        model.addAttribute("paginaResolucoes", paginaResolucoes);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);

        return "resolucao/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("resolucao", new Resolucao());
        return "resolucao/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("resolucao", resolucaoService.buscarPorId(id).orElse(new Resolucao()));
        return "resolucao/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("resolucao") Resolucao resolucao, RedirectAttributes redirectAttributes) {
        try {
            resolucaoService.salvar(resolucao);
            redirectAttributes.addFlashAttribute("sucesso", "Resolução salva com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Resolução.");
        }
        return "redirect:/resolucoes";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            resolucaoService.revogar(id); // Usa o novo método
            redirectAttributes.addFlashAttribute("sucesso", "Resolução revogada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao revogar Resolução.");
        }
        return "redirect:/resolucoes";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            resolucaoService.restaurar(id);
            redirectAttributes.addFlashAttribute("sucesso", "Resolução restaurada (Em Vigor) com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao restaurar Resolução.");
        }
        return "redirect:/resolucoes";
    }

    @GetMapping("/deletar/{id}")
    public String deletar(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            resolucaoService.excluirFisicamente(id);
            ra.addFlashAttribute("sucesso", "Resolução excluída definitivamente.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao excluir resolução.");
        }
        return "redirect:/resolucoes";
    }
}