package br.com.cremepe.jeton.controlador;

import java.util.List;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dominio.GestaoConselheiro;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gestao-conselheiros")
public class GestaoConselheiroController {

    @Autowired
    private GestaoConselheiroService gestaoConselheiroService;
    @Autowired
    private GestaoService gestaoService;
    @Autowired
    private ConselheiroService conselheiroService;
    @Autowired
    private AtividadeConselhalRepository atividadeRepository;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "gestao.nomeGestao") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "asc") String dir,
            Model model, HttpSession session) {

        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";

        Page<GestaoConselheiro> pagina = gestaoConselheiroService.listarComPaginacaoEPesquisa(termo, situacao, page,
                size, sort, dir);

        model.addAttribute("paginaVinculos", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);

        return "gestaoconselheiro/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";
        model.addAttribute("gestaoConselheiro", new GestaoConselheiro());
        carregarListasDeApoio(model);
        return "gestaoconselheiro/formulario";
    }

    @GetMapping("/alternar-status/{idGestao}/{idPessoa}")
    public String alternarStatus(@PathVariable("idGestao") Integer idGestao,
            @PathVariable("idPessoa") Integer idPessoa,
            RedirectAttributes ra, HttpSession session) {

        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";

        try {
            // 1. Busca o vínculo atual no banco
            GestaoConselheiro vinculo = gestaoConselheiroService.buscarPorId(idGestao, idPessoa)
                    .orElseThrow(() -> new IllegalArgumentException("Vínculo não encontrado."));

            // 2. Inverte o status: Se for Ativo vira Inativo, e vice-versa
            if ("A".equals(vinculo.getInSituacao())) {
                vinculo.setInSituacao("I");
                ra.addFlashAttribute("sucesso", "O vínculo foi INATIVADO com sucesso.");
            } else {
                vinculo.setInSituacao("A");
                ra.addFlashAttribute("sucesso",
                        "O vínculo foi ATIVADO com sucesso. (Outras gestões foram inativadas automaticamente)");
            }

            // 3. Salva a alteração (Isto aciona a regra inteligente do
            // GestaoConselheiroService)
            gestaoConselheiroService.salvar(vinculo);

        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao tentar alternar o status do vínculo.");
        }

        return "redirect:/gestao-conselheiros";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("gestaoConselheiro") GestaoConselheiro vinculo, RedirectAttributes ra) {
        try {
            gestaoConselheiroService.salvar(vinculo);
            ra.addFlashAttribute("sucesso", "Vínculo de conselheiro guardado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao guardar o vínculo: O médico já pode estar alocado a esta gestão.");
        }
        return "redirect:/gestao-conselheiros";
    }

    @GetMapping("/excluir/{idGestao}/{idPessoa}")
    public String excluir(@PathVariable("idGestao") Integer idGestao,
            @PathVariable("idPessoa") Integer idPessoa,
            RedirectAttributes ra) {
        try {
            gestaoConselheiroService.excluir(idGestao, idPessoa);
            ra.addFlashAttribute("sucesso", "Vínculo removido com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro",
                    "Não foi possível remover o vínculo. O conselheiro já possui atividades nesta gestão.");
        }
        return "redirect:/gestao-conselheiros";
    }

    private void carregarListasDeApoio(Model model) {
        // Aproveitamos os serviços já criados para alimentar as caixas de selecção
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
    }

    @GetMapping("/vincular/{idGestao}")
    public String prepararVincularMassa(@PathVariable("idGestao") Integer idGestao, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";

        Gestao gestao = gestaoService.buscarPorId(idGestao).orElseThrow();
        List<Conselheiro> todosConselheiros = conselheiroService.listarTodos();

        // Pega apenas os IDs de quem já está vinculado para marcar o checkbox
        List<Integer> selecionadosIds = gestaoConselheiroService.listarTodos().stream()
                .filter(v -> v.getId().getIdGestao().equals(idGestao))
                .map(v -> v.getId().getIdPessoa())
                .toList();

        List<Integer> idsComAtividades = selecionadosIds.stream()
                .filter(idPessoa -> atividadeRepository.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao,
                        idPessoa) > 0)
                .toList();

        model.addAttribute("idsComAtividades", idsComAtividades);
        model.addAttribute("gestao", gestao);
        model.addAttribute("conselheiros", todosConselheiros);
        model.addAttribute("selecionadosIds", selecionadosIds);

        return "gestaoconselheiro/vincular-massa";
    }

    @PostMapping("/vincular/salvar")
    public String salvarVincularMassa(@RequestParam("idGestao") Integer idGestao,
            @RequestParam(value = "conselheirosIds", required = false) List<Integer> conselheirosIds,
            RedirectAttributes ra) {
        try {
            gestaoConselheiroService.atualizarVinculosEmMassa(idGestao, conselheirosIds);
            ra.addFlashAttribute("sucesso", "Vínculos atualizados com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao atualizar vínculos.");
        }
        return "redirect:/gestoes";
    }
}