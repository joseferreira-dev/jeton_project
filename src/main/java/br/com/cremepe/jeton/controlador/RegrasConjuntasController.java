package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.RegrasConjuntas;
import br.com.cremepe.jeton.servico.RegrasConjuntasService; // Certifique-se de ter este Service
import br.com.cremepe.jeton.servico.RegrasService; // Para popular as caixas de seleção
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

    @Autowired private RegrasConjuntasService regrasConjuntasService;
    @Autowired private RegrasService regrasService;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "tipoLimite", required = false, defaultValue = "") String tipoLimite,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "nomeRegra") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "asc") String dir,
            Model model, HttpSession session) {
            
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";

        Page<RegrasConjuntas> paginaRegrasConjuntas = regrasConjuntasService.listarComPaginacaoEPesquisa(termo, tipoLimite, page, size, sort, dir);

        model.addAttribute("paginaRegrasConjuntas", paginaRegrasConjuntas);
        model.addAttribute("termo", termo);
        model.addAttribute("tipoLimite", tipoLimite);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);

        return "regrasconjuntas/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("regrasConjuntas", new RegrasConjuntas());
        model.addAttribute("listaRegras", regrasService.listarTodos());
        return "regrasconjuntas/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("regrasConjuntas", regrasConjuntasService.buscarPorId(id).orElse(new RegrasConjuntas()));
        model.addAttribute("listaRegras", regrasService.listarTodos());
        return "regrasconjuntas/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("regrasConjuntas") RegrasConjuntas regrasConjuntas, RedirectAttributes redirectAttributes) {
        try {
            regrasConjuntasService.salvar(regrasConjuntas);
            redirectAttributes.addFlashAttribute("sucesso", "Regras Conjuntas salvas com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Regras Conjuntas.");
        }
        return "redirect:/regras-conjuntas";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            regrasConjuntasService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Removido com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover.");
        }
        return "redirect:/regras-conjuntas";
    }
}