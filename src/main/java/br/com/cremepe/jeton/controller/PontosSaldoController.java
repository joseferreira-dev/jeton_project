package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.PontosSaldo;
import br.com.cremepe.jeton.service.ConselheiroService;
import br.com.cremepe.jeton.service.GestaoService;
import br.com.cremepe.jeton.service.PontosSaldoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pontos-saldo")
@PreAuthorize("hasAuthority('P') or hasAuthority('S')")
public class PontosSaldoController {

    private final PontosSaldoService pontosService;
    private final ConselheiroService conselheiroService;
    private final GestaoService gestaoService;

    PontosSaldoController(PontosSaldoService pontosService, ConselheiroService conselheiroService,
            GestaoService gestaoService) {
        this.pontosService = pontosService;
        this.conselheiroService = conselheiroService;
        this.gestaoService = gestaoService;
    }

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("lista", pontosService.listarTodos());
        return "pontosremanescentes/lista";
    }

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

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("pontos") PontosSaldo pontos,
            HttpSession session,
            RedirectAttributes ra) {
        boolean isNovo = pontos.getIdPontosSaldo() == null;

        if (isNovo) {
            pontosService.criar(pontos);
            ra.addFlashAttribute("sucesso", "Registro de pontos criado com sucesso!");
        } else {
            pontosService.atualizar(pontos);
            ra.addFlashAttribute("sucesso", "Registro de pontos atualizado com sucesso!");
        }
        return "redirect:/pontos-saldo";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id,
            HttpSession session,
            RedirectAttributes ra) {
        pontosService.excluir(id);
        ra.addFlashAttribute("sucesso", "Registro de pontos removido com sucesso!");
        return "redirect:/pontos-saldo";
    }

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }

    private void carregarListasDeApoio(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
    }
}