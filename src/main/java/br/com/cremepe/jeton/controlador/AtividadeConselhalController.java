package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.servico.AtividadeConselhalService;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.RegrasService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/atividades")
public class AtividadeConselhalController {

    @Autowired private AtividadeConselhalService atividadeService;
    
    // Injeções para popular os comboboxes (selects) da tela
    @Autowired private ConselheiroService conselheiroService;
    @Autowired private GestaoService gestaoService;
    @Autowired private RegrasService regrasService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", atividadeService.listarTodos());
        return "atividadeconselhal/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("atividade", new AtividadeConselhal());
        carregarListasDeApoio(model);
        return "atividadeconselhal/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("atividade", atividadeService.buscarPorId(id).orElse(new AtividadeConselhal()));
        carregarListasDeApoio(model);
        return "atividadeconselhal/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("atividade") AtividadeConselhal atividade, RedirectAttributes redirectAttributes) {
        try {
            // CORREÇÃO: Chamando o método com o nome exato que está no Service
            atividadeService.salvarAtividade(atividade);
            redirectAttributes.addFlashAttribute("sucesso", "Atividade salva com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/atividades";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            // CORREÇÃO: Chamando o método com o nome exato que está no Service
            atividadeService.excluirAtividade(id);
            redirectAttributes.addFlashAttribute("sucesso", "Atividade removida com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover atividade.");
        }
        return "redirect:/atividades";
    }
    
    // Método auxiliar para popular os <select> do formulário HTML
    private void carregarListasDeApoio(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("listaRegras", regrasService.listarTodos()); // Utiliza o método padrão do RegrasService
    }
}