package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dominio.GestaoConselheiro;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/gestao-conselheiros")
public class GestaoConselheiroController {

    private static final Logger log = LoggerFactory.getLogger(GestaoConselheiroController.class);

    @Autowired
    private GestaoConselheiroService gestaoConselheiroService;
    @Autowired
    private GestaoService gestaoService;
    @Autowired
    private ConselheiroService conselheiroService;
    @Autowired
    private AtividadeConselhalRepository atividadeRepository;

    // =========================================================================
    // LISTAGEM
    // =========================================================================
    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "gestao.nomeGestao") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "asc") String dir,
            Model model, HttpSession session) {

        if (naoAutenticado(session))
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

    // =========================================================================
    // FORMULÁRIOS (NOVO / EDIÇÃO)
    // =========================================================================
    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("gestaoConselheiro", new GestaoConselheiro());
        carregarListasDeApoio(model);
        return "gestaoconselheiro/formulario";
    }

    @GetMapping("/alternar-status/{idGestao}/{idPessoa}")
    public String alternarStatus(@PathVariable("idGestao") Integer idGestao,
            @PathVariable("idPessoa") Integer idPessoa,
            RedirectAttributes ra, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";

        try {
            GestaoConselheiro vinculo = gestaoConselheiroService.buscarOuFalhar(idGestao, idPessoa);
            if (vinculo.isAtivo()) {
                gestaoConselheiroService.inativarVinculo(idGestao, idPessoa);
                ra.addFlashAttribute("sucesso", "O vínculo foi INATIVADO com sucesso.");
            } else {
                gestaoConselheiroService.ativarVinculo(idGestao, idPessoa);
                ra.addFlashAttribute("sucesso",
                        "O vínculo foi ATIVADO com sucesso. (Outras gestões foram inativadas automaticamente)");
            }
        } catch (Exception e) {
            log.error("Erro ao alternar status do vínculo {}/{}: {}", idGestao, idPessoa, e.getMessage());
            ra.addFlashAttribute("erro", "Erro ao tentar alternar o status do vínculo.");
        }
        return "redirect:/gestao-conselheiros";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("gestaoConselheiro") GestaoConselheiro vinculo,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Integer idGestao = vinculo.getGestao().getIdGestao();
            Integer idPessoa = vinculo.getConselheiro().getIdPessoa();

            // Verifica se o vínculo já existe
            boolean exists = gestaoConselheiroService.buscarPorId(idGestao, idPessoa).isPresent();

            if (!exists) {
                gestaoConselheiroService.criar(vinculo);
                ra.addFlashAttribute("sucesso", "Vínculo de conselheiro criado com sucesso!");
            } else {
                gestaoConselheiroService.atualizar(vinculo);
                ra.addFlashAttribute("sucesso", "Vínculo de conselheiro atualizado com sucesso!");
            }
        } catch (Exception e) {
            log.error("Erro ao salvar vínculo: {}", e.getMessage());
            ra.addFlashAttribute("erro", "Erro ao guardar o vínculo: " + e.getMessage());
        }
        return "redirect:/gestao-conselheiros";
    }

    @GetMapping("/excluir/{idGestao}/{idPessoa}")
    public String excluir(@PathVariable("idGestao") Integer idGestao,
            @PathVariable("idPessoa") Integer idPessoa,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            gestaoConselheiroService.excluir(idGestao, idPessoa);
            ra.addFlashAttribute("sucesso", "Vínculo removido com sucesso!");
        } catch (Exception e) {
            log.error("Erro ao excluir vínculo {}/{}: {}", idGestao, idPessoa, e.getMessage());
            ra.addFlashAttribute("erro",
                    "Não foi possível remover o vínculo. O conselheiro já possui atividades nesta gestão.");
        }
        return "redirect:/gestao-conselheiros";
    }

    // =========================================================================
    // VÍNCULO EM MASSA
    // =========================================================================
    @GetMapping("/vincular/{idGestao}")
    public String prepararVincularMassa(@PathVariable("idGestao") Integer idGestao, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";

        Gestao gestao = gestaoService.buscarPorId(idGestao)
                .orElseThrow(() -> new IllegalArgumentException("Gestão não encontrada"));
        List<Conselheiro> todosConselheiros = conselheiroService.listarTodos();

        // IDs já vinculados (todos, independente do status)
        List<Integer> selecionadosIds = gestaoConselheiroService.listarTodos().stream()
                .filter(v -> v.getId().getIdGestao().equals(idGestao))
                .map(v -> v.getId().getIdPessoa())
                .toList();

        // IDs que possuem atividades (para desabilitar remoção)
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
            HttpSession session,
            RedirectAttributes ra) {
        try {
            gestaoConselheiroService.atualizarVinculosEmMassa(idGestao, conselheirosIds);
            ra.addFlashAttribute("sucesso", "Vínculos atualizados com sucesso!");
        } catch (Exception e) {
            log.error("Erro ao atualizar vínculos em massa para gestão {}: {}", idGestao, e.getMessage());
            ra.addFlashAttribute("erro", "Erro ao atualizar vínculos.");
        }
        return "redirect:/gestoes";
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================
    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }

    private void carregarListasDeApoio(Model model) {
        model.addAttribute("listaGestoes", gestaoService.listarTodos());

        List<Conselheiro> conselheirosOrdenados = conselheiroService.listarTodos().stream()
                .sorted(Comparator.comparing(c -> c.getPessoa().getNome()))
                .collect(Collectors.toList());
        model.addAttribute("listaConselheiros", conselheirosOrdenados);
    }
}