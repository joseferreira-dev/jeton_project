package br.com.cremepe.jeton.controlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.servico.GestaoService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestoes")
public class GestaoController {

    @Autowired
    private GestaoService gestaoService;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "dtInicio") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "desc") String dir,
            Model model, HttpSession session) {

        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";

        // Chama sem a variável situacao
        Page<Gestao> pagina = gestaoService.listarComPaginacaoEPesquisa(termo, page, size, sort, dir);

        model.addAttribute("paginaGestoes", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        
        return "gestao/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        model.addAttribute("gestao", new Gestao());
        return "gestao/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        Gestao gestao = gestaoService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Gestão inválida:" + id));
        model.addAttribute("gestao", gestao);
        return "gestao/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("gestao") Gestao gestao, RedirectAttributes ra) {
        try {
            gestaoService.salvar(gestao);
            ra.addFlashAttribute("sucesso", "Gestão salva com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/gestoes";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            gestaoService.excluir(id);
            ra.addFlashAttribute("sucesso", "Gestão removida com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Não foi possível remover. Verifique se existem conselheiros ou atividades vinculadas a esta gestão.");
        }
        return "redirect:/gestoes";
    }
}