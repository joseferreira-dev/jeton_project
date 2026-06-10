package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.service.PortariaService;
import br.com.cremepe.jeton.service.RegrasService;
import br.com.cremepe.jeton.service.ResolucaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/regras")
@PreAuthorize("hasAuthority('R') or hasAuthority('S')")
public class RegrasController {

    @Autowired
    private RegrasService regrasService;
    @Autowired
    private PortariaService portariaService;
    @Autowired
    private ResolucaoService resolucaoService;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "judicante", required = false, defaultValue = "") String judicante,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "nomeRegra") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "asc") String dir,
            Model model, HttpSession session) {

        if (naoAutenticado(session))
            return "redirect:/login";

        Page<Regras> pagina = regrasService.listarComPaginacaoEPesquisa(termo, situacao, judicante, page, size, sort,
                dir);
        model.addAttribute("paginaRegras", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("judicante", judicante);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "regras/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("regra", new Regras());
        carregarApoioFormulario(model);
        return "regras/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        Regras regra = regrasService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Regra não encontrada"));
        model.addAttribute("regra", regra);
        carregarApoioFormulario(model);
        return "regras/formulario";
    }

    private void carregarApoioFormulario(Model model) {
        model.addAttribute("listaPortarias", portariaService.listarTodos().stream()
                .filter(p -> p.isEmVigor())
                .collect(Collectors.toList()));
        model.addAttribute("listaResolucoes", resolucaoService.listarTodos().stream()
                .filter(r -> r.isEmVigor())
                .collect(Collectors.toList()));
    }

    @GetMapping("/regras-por-resolucao")
    @ResponseBody
    public List<Map<String, Object>> getRegrasPorResolucao(@RequestParam(required = false) Integer resolucaoId) {
        List<Regras> regras = regrasService.listarRegrasPorResolucao(resolucaoId);
        return regras.stream()
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getIdRegra());
                    map.put("nome", r.getNomeRegra());
                    map.put("pontos", r.getPontos());
                    map.put("revogado", r.getInRevogado());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("regra") Regras regra,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            boolean isNovo = regra.getIdRegra() == null;

            if (isNovo) {
                regrasService.criar(regra);
                ra.addFlashAttribute("sucesso", "Regra criada com sucesso!");
            } else {
                regrasService.atualizar(regra);
                ra.addFlashAttribute("sucesso", "Regra atualizada com sucesso!");
            }
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao salvar a regra.");
        }
        return "redirect:/regras";
    }

    @GetMapping("/revogar/{id}")
    public String revogar(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            regrasService.revogar(id);
            ra.addFlashAttribute("sucesso", "Regra revogada com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao revogar regra.");
        }
        return "redirect:/regras";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            regrasService.restaurar(id);
            ra.addFlashAttribute("sucesso", "Regra restaurada (em vigor) com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao restaurar regra.");
        }
        return "redirect:/regras";
    }

    @GetMapping("/deletar/{id}")
    public String deletar(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            regrasService.excluir(id);
            ra.addFlashAttribute("sucesso", "Regra excluída definitivamente.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao excluir regra.");
        }
        return "redirect:/regras";
    }

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }
}