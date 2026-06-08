package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.Pessoa;
import br.com.cremepe.jeton.servico.ConselheiroService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/conselheiros")
public class ConselheiroController {

    @Autowired
    private ConselheiroService conselheiroService;

    // =========================================================================
    // LISTAGEM
    // =========================================================================
    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "pessoa.nome") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "asc") String dir,
            Model model, HttpSession session) {

        if (naoAutenticado(session))
            return "redirect:/login";

        Page<Conselheiro> pagina = conselheiroService.listarComPaginacaoEPesquisa(termo, situacao, page, size, sort,
                dir);

        model.addAttribute("paginaConselheiros", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "conselheiro/lista";
    }

    // =========================================================================
    // FORMULÁRIOS (NOVO / EDIÇÃO)
    // =========================================================================
    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";

        Conselheiro conselheiro = new Conselheiro();
        conselheiro.setPessoa(new Pessoa());
        model.addAttribute("conselheiro", conselheiro);
        return "conselheiro/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";

        Conselheiro conselheiro = conselheiroService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Conselheiro não encontrado para o ID: " + id));
        model.addAttribute("conselheiro", conselheiro);
        return "conselheiro/formulario";
    }

    // =========================================================================
    // SALVAR (CRIAR / ATUALIZAR)
    // =========================================================================
    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("conselheiro") Conselheiro conselheiro,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            if (conselheiro.getIdPessoa() == null) {
                conselheiroService.criar(conselheiro);
            } else {
                conselheiroService.atualizar(conselheiro);
            }

            ra.addFlashAttribute("sucesso", "Conselheiro gravado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao gravar: " + e.getMessage());
        }
        return "redirect:/conselheiros";
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================
    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            conselheiroService.excluir(id);
            ra.addFlashAttribute("sucesso", "Conselheiro removido com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro",
                    "Não foi possível remover. Este conselheiro possui registros ou atividades vinculadas.");
        }
        return "redirect:/conselheiros";
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================
    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }
}