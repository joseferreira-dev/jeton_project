package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.servico.RegrasService;
import br.com.cremepe.jeton.servico.PortariaService;
import br.com.cremepe.jeton.servico.ResolucaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/regras")
public class RegrasController {

    @Autowired
    private RegrasService regrasService;
    @Autowired
    private PortariaService portariaService;
    @Autowired
    private ResolucaoService resolucaoService;

    // =========================================================================
    // LISTAGEM
    // =========================================================================
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

    // =========================================================================
    // FORMULÁRIOS
    // =========================================================================
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

    // =========================================================================
    // SALVAR (CRIAR / ATUALIZAR)
    // =========================================================================
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("regra") Regras regra,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Integer idUsuarioLogado = getIdUsuarioLogado(session);
            regrasService.salvar(regra, idUsuarioLogado);
            ra.addFlashAttribute("sucesso", "Regra salva com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao salvar a regra.");
        }
        return "redirect:/regras";
    }

    // =========================================================================
    // REVOGAÇÃO (SOFT DELETE)
    // =========================================================================
    @GetMapping("/revogar/{id}")
    public String revogar(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Integer idUsuarioLogado = getIdUsuarioLogado(session);
            regrasService.revogar(id, idUsuarioLogado);
            ra.addFlashAttribute("sucesso", "Regra revogada com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao revogar regra.");
        }
        return "redirect:/regras";
    }

    // =========================================================================
    // RESTAURAR
    // =========================================================================
    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Integer idUsuarioLogado = getIdUsuarioLogado(session);
            regrasService.restaurar(id, idUsuarioLogado);
            ra.addFlashAttribute("sucesso", "Regra restaurada (em vigor) com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao restaurar regra.");
        }
        return "redirect:/regras";
    }

    // =========================================================================
    // EXCLUSÃO PERMANENTE
    // =========================================================================
    @GetMapping("/deletar/{id}")
    public String deletarFisicamente(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Integer idUsuarioLogado = getIdUsuarioLogado(session);
            regrasService.excluirFisicamente(id, idUsuarioLogado);
            ra.addFlashAttribute("sucesso", "Regra excluída definitivamente.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao excluir regra.");
        }
        return "redirect:/regras";
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