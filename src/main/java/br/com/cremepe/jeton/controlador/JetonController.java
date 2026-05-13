package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Jeton;
import br.com.cremepe.jeton.servico.JetonService;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/jeton")
public class JetonController {

    @Autowired private JetonService jetonService;
    @Autowired private ConselheiroService conselheiroService;
    @Autowired private GestaoService gestaoService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", jetonService.listarTodos());
        return "jeton/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("jeton", new Jeton());
        carregarListasDeApoio(model);
        return "jeton/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("jeton", jetonService.buscarPorId(id).orElse(new Jeton()));
        carregarListasDeApoio(model);
        return "jeton/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("jeton") Jeton jeton, RedirectAttributes redirectAttributes) {
        try {
            jetonService.salvar(jeton);
            redirectAttributes.addFlashAttribute("sucesso", "Jeton salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar Jeton: " + e.getMessage());
        }
        return "redirect:/jeton";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            // CORREÇÃO: Chamando o método com o nome correto presente no Service
            jetonService.excluirJeton(id);
            redirectAttributes.addFlashAttribute("sucesso", "Jeton removido com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover Jeton.");
        }
        return "redirect:/jeton";
    }

    private void carregarListasDeApoio(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
    }
}