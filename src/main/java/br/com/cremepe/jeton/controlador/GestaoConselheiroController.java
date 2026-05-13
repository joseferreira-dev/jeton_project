package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.GestaoConselheiro;
import br.com.cremepe.jeton.servico.GestaoConselheiroService; // Certifique-se de ter este Service
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.ConselheiroService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gestao-conselheiros")
public class GestaoConselheiroController {

    @Autowired private GestaoConselheiroService gestaoConselheiroService;
    @Autowired private GestaoService gestaoService;
    @Autowired private ConselheiroService conselheiroService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("lista", gestaoConselheiroService.listarTodos());
        return "gestaoconselheiro/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("gestaoConselheiro", new GestaoConselheiro());
        carregarListasDeApoio(model);
        return "gestaoconselheiro/formulario";
    }

    // Nota: Como a chave primária é composta (GestaoConselheiroId), o método de edição 
    // pode exigir a passagem de dois IDs na URL, ou ser adaptado conforme sua regra de negócio.
    // Aqui estou assumindo um cenário de recadastro para simplificar o formulário.
    
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("gestaoConselheiro") GestaoConselheiro gestaoConselheiro, RedirectAttributes redirectAttributes) {
        try {
            gestaoConselheiroService.salvar(gestaoConselheiro);
            redirectAttributes.addFlashAttribute("sucesso", "Vínculo salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar vínculo.");
        }
        return "redirect:/gestao-conselheiros";
    }

    private void carregarListasDeApoio(Model model) {
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
    }
}