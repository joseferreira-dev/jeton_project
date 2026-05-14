package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.servico.AtividadeConselhalService;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.RegrasService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/atividades")
public class AtividadeConselhalController {

    @Autowired private AtividadeConselhalService atividadeService;
    @Autowired private ConselheiroService conselheiroService;
    @Autowired private GestaoService gestaoService;
    @Autowired private RegrasService regrasService;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "turno", required = false, defaultValue = "") String turno,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "dataHoraAtividade") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "desc") String dir,
            Model model, HttpSession session) {

        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";

        Page<AtividadeConselhal> pagina = atividadeService.listarComPaginacaoEPesquisa(termo, situacao, turno, page, size, sort, dir);

        model.addAttribute("paginaAtividades", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("turno", turno);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        
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
        AtividadeConselhal atividade = atividadeService.buscarPorId(id).orElseThrow();
        model.addAttribute("atividade", atividade);
        carregarListasDeApoio(model);
        return "atividadeconselhal/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("atividade") AtividadeConselhal atividade, RedirectAttributes ra) {
        try {
            atividadeService.salvarAtividade(atividade);
            ra.addFlashAttribute("sucesso", "Atividade registada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao guardar: " + e.getMessage());
        }
        return "redirect:/atividades";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            atividadeService.excluirAtividade(id);
            ra.addFlashAttribute("sucesso", "Atividade removida com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao remover atividade. Pode estar vinculada a pagamentos de Jeton.");
        }
        return "redirect:/atividades";
    }
    
    private void carregarListasDeApoio(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        // Traz apenas regras ativas para não poluir o select
        model.addAttribute("listaRegras", regrasService.listarTodos().stream().filter(r -> "N".equals(r.getInRevogado())).toList());
    }
}