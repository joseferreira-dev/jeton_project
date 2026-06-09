package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.service.NivelAcessoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/niveis-acesso")
public class NivelAcessoController {

    private static final Logger log = LoggerFactory.getLogger(NivelAcessoController.class);

    @Autowired
    private NivelAcessoService nivelAcessoService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("lista", nivelAcessoService.listarTodos());
        return "nivelacesso/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("nivelAcesso", new NivelAcesso());
        return "nivelacesso/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") String id, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        NivelAcesso nivel = nivelAcessoService.buscarOuFalhar(id);
        model.addAttribute("nivelAcesso", nivel);
        return "nivelacesso/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("nivelAcesso") NivelAcesso nivel,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            boolean isNovo = (nivel.getIdNivel() == null || nivel.getIdNivel().trim().isEmpty())
                    || !nivelAcessoService.buscarPorId(nivel.getIdNivel()).isPresent();

            if (isNovo) {
                nivelAcessoService.criar(nivel);
                redirectAttributes.addFlashAttribute("sucesso", "Nível de Acesso criado com sucesso!");
            } else {
                nivelAcessoService.atualizar(nivel);
                redirectAttributes.addFlashAttribute("sucesso", "Nível de Acesso atualizado com sucesso!");
            }
        } catch (Exception e) {
            log.error("Erro ao salvar nível de acesso: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/niveis-acesso";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") String id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            nivelAcessoService.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Nível de Acesso removido!");
        } catch (DataIntegrityViolationException e) {
            log.error("Erro de integridade ao excluir nível {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("erro",
                    "Não é possível remover este nível pois ele está associado a um ou mais usuários.");
        } catch (Exception e) {
            log.error("Erro ao excluir nível {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover Nível de Acesso.");
        }
        return "redirect:/niveis-acesso";
    }

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }
}