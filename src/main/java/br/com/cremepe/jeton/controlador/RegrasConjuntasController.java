package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.RegrasConjuntas;
import br.com.cremepe.jeton.servico.RegrasConjuntasService;
import br.com.cremepe.jeton.servico.RegrasService;
import br.com.cremepe.jeton.servico.ResolucaoService;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.stream.Collectors;

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
    @Autowired
    private ResolucaoService resolucaoService;

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
        model.addAttribute("listaRegras", regrasService.listarTodos());
        model.addAttribute("listaResolucoes", resolucaoService.listarTodos()); // <-- adicionar
        return "regrasconjuntas/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        RegrasConjuntas regra = regrasConjuntasService.buscarOuFalhar(id);
        model.addAttribute("regrasConjuntas", regra);
        model.addAttribute("listaRegras", regrasService.listarTodos());
        model.addAttribute("listaResolucoes", resolucaoService.listarTodos());

        // IDs das regras já selecionadas – converte para string separada por vírgulas
        List<Integer> idsSelecionados = regra.getRegrasAgrupadas().stream()
                .map(Regras::getIdRegra)
                .collect(Collectors.toList());
        String idsSelecionadosStr = idsSelecionados.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        model.addAttribute("regrasSelecionadasIds", idsSelecionadosStr);

        return "regrasconjuntas/formulario";
    }

    // =========================================================================
    // SALVAR
    // =========================================================================
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("regrasConjuntas") RegrasConjuntas regrasConjuntas,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            boolean isNovo = regrasConjuntas.getIdRegraConjunta() == null;

            if (isNovo) {
                regrasConjuntasService.criar(regrasConjuntas);
                ra.addFlashAttribute("sucesso", "Regras Conjuntas criadas com sucesso!");
            } else {
                regrasConjuntasService.atualizar(regrasConjuntas);
                ra.addFlashAttribute("sucesso", "Regras Conjuntas atualizadas com sucesso!");
            }
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
            regrasConjuntasService.excluir(id);
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
}