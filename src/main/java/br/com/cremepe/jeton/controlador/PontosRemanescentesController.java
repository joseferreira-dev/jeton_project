package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.PontosSaldo;
import br.com.cremepe.jeton.servico.PontosRemanescentesService;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pontos-remanescentes")
public class PontosRemanescentesController {

    @Autowired private PontosRemanescentesService pontosService; 
    
    @Autowired private ConselheiroService conselheiroService;
    @Autowired private GestaoService gestaoService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", pontosService.listarTodos());
        return "pontosremanescentes/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("pontos", new PontosSaldo());
        carregarListasDeApoio(model);
        return "pontosremanescentes/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("pontos", pontosService.buscarPorId(id).orElse(new PontosSaldo()));
        carregarListasDeApoio(model);
        return "pontosremanescentes/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("pontos") PontosSaldo pontos, RedirectAttributes redirectAttributes) {
        try {
            pontosService.salvar(pontos);
            redirectAttributes.addFlashAttribute("sucesso", "Registro salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/pontos-remanescentes";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            pontosService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Registro removido com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover.");
        }
        return "redirect:/pontos-remanescentes";
    }

    private void carregarListasDeApoio(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
    }
}