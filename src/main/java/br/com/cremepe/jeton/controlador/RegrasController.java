package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Regras;
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

    @Autowired private RegrasService regrasService;
    @Autowired private PortariaService portariaService;
    @Autowired private ResolucaoService resolucaoService;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "judicante", required = false, defaultValue = "") String judicante, // NOVO
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "nomeRegra") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "asc") String dir,
            Model model, HttpSession session) {
            
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";

        Page<Regras> paginaRegras = regrasService.listarComPaginacaoEPesquisa(termo, situacao, judicante, page, size, sort, dir);

        model.addAttribute("paginaRegras", paginaRegras);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("judicante", judicante); // NOVO
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);

        return "regras/lista";
    }

    private void carregarApoioFormulario(Model model) {
        // Carrega apenas as normativas que estão "Em Vigor" para o dropdown
        model.addAttribute("listaPortarias", portariaService.listarTodos().stream().filter(p -> "N".equals(p.getInRevogado())).collect(Collectors.toList()));
        model.addAttribute("listaResolucoes", resolucaoService.listarTodos().stream().filter(r -> "N".equals(r.getInRevogado())).collect(Collectors.toList()));
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("regra", new Regras());
        carregarApoioFormulario(model);
        return "regras/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("regra", regrasService.buscarPorId(id).orElse(new Regras()));
        carregarApoioFormulario(model);
        return "regras/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("regras") Regras regra, RedirectAttributes ra) {
        try {
            regrasService.salvar(regra);
            ra.addFlashAttribute("sucesso", "Regra salva com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Ocorreu um erro inesperado ao processar a regra.");
        }
        return "redirect:/regras";
    }

    @GetMapping("/excluir/{id}")
    public String revogar(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try { 
            regrasService.revogar(id); 
            ra.addFlashAttribute("sucesso", "Regra revogada com sucesso!"); 
        } catch (Exception e) { 
            ra.addFlashAttribute("erro", "Erro ao revogar regra."); 
        }
        return "redirect:/regras";
    }

    @GetMapping("/restaurar/{id}")
    public String restaurar(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try { 
            regrasService.restaurar(id); 
            ra.addFlashAttribute("sucesso", "Regra restaurada (Em Vigor)!"); 
        } catch (Exception e) { 
            ra.addFlashAttribute("erro", "Erro ao restaurar regra."); 
        }
        return "redirect:/regras";
    }

    @GetMapping("/deletar/{id}")
    public String deletarFisicamente(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try { 
            regrasService.excluirFisicamente(id); 
            ra.addFlashAttribute("sucesso", "Regra excluída definitivamente!"); 
        } catch (RuntimeException e) { 
            ra.addFlashAttribute("erro", e.getMessage()); 
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao tentar excluir."); 
        }
        return "redirect:/regras";
    }
}