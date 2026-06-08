package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.PontosSaldo;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.PontosRemanescentesService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pontos-remanescentes")
public class PontosRemanescentesController {

    @Autowired
    private PontosRemanescentesService pontosService;
    @Autowired
    private ConselheiroService conselheiroService;
    @Autowired
    private GestaoService gestaoService;

    // =========================================================================
    // LISTAGEM
    // =========================================================================
    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("lista", pontosService.listarTodos());
        return "pontosremanescentes/lista";
    }

    // =========================================================================
    // FORMULÁRIOS (NOVO / EDIÇÃO)
    // =========================================================================
    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("pontos", new PontosSaldo());
        carregarListasDeApoio(model);
        return "pontosremanescentes/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        PontosSaldo pontos = pontosService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro de pontos não encontrado"));
        model.addAttribute("pontos", pontos);
        carregarListasDeApoio(model);
        return "pontosremanescentes/formulario";
    }

    // =========================================================================
    // SALVAR (CRIAR / ATUALIZAR)
    // =========================================================================
    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("pontos") PontosSaldo pontos,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            boolean isNovo = pontos.getIdPontosSaldo() == null;

            if (isNovo) {
                pontosService.criar(pontos);
                ra.addFlashAttribute("sucesso", "Registro de pontos criado com sucesso!");
            } else {
                pontosService.atualizar(pontos);
                ra.addFlashAttribute("sucesso", "Registro de pontos atualizado com sucesso!");
            }
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao salvar o registro.");
        }
        return "redirect:/pontos-remanescentes";
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================
    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            pontosService.excluir(id);
            ra.addFlashAttribute("sucesso", "Registro de pontos removido com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao remover o registro.");
        }
        return "redirect:/pontos-remanescentes";
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================
    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }

    private void carregarListasDeApoio(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
    }
}