package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.RegrasConjuntas;
import br.com.cremepe.jeton.service.RegrasConjuntasService;
import br.com.cremepe.jeton.service.RegrasService;
import br.com.cremepe.jeton.service.ResolucaoService;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/regras-conjuntas")
@PreAuthorize("hasAuthority('R') or hasAuthority('S')")
public class RegrasConjuntasController {

    private final RegrasConjuntasService regrasConjuntasService;
    private final RegrasService regrasService;
    private final ResolucaoService resolucaoService;

    RegrasConjuntasController(RegrasConjuntasService regrasConjuntasService, RegrasService regrasService,
            ResolucaoService resolucaoService) {
        this.regrasConjuntasService = regrasConjuntasService;
        this.regrasService = regrasService;
        this.resolucaoService = resolucaoService;
    }

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

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("regrasConjuntas", new RegrasConjuntas());
        model.addAttribute("listaRegras", regrasService.listarTodos());
        model.addAttribute("listaResolucoes", resolucaoService.listarTodos());
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

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("regrasConjuntas") RegrasConjuntas regrasConjuntas,
            HttpSession session,
            RedirectAttributes ra) {
        boolean isNovo = regrasConjuntas.getIdRegraConjunta() == null;

        if (isNovo) {
            regrasConjuntasService.criar(regrasConjuntas);
            ra.addFlashAttribute("sucesso", "Regras Conjuntas criadas com sucesso!");
        } else {
            regrasConjuntasService.atualizar(regrasConjuntas);
            ra.addFlashAttribute("sucesso", "Regras Conjuntas atualizadas com sucesso!");
        }
        return "redirect:/regras-conjuntas";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        regrasConjuntasService.excluir(id);
        ra.addFlashAttribute("sucesso", "Regra Conjunta removida com sucesso!");
        return "redirect:/regras-conjuntas";
    }

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }
}