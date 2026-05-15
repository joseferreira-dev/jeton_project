package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.servico.AtividadeConselhalService;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.RegrasService;
import br.com.cremepe.jeton.repositorio.GestaoConselheiroRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/atividades")
public class AtividadeConselhalController {

    @Autowired private AtividadeConselhalService atividadeService;
    @Autowired private ConselheiroService conselheiroService;
    @Autowired private GestaoService gestaoService;
    @Autowired private RegrasService regrasService;
    @Autowired private GestaoConselheiroRepository gestaoConselheiroRepository;

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
            
        } catch (RuntimeException e) {
            // Captura as nossas mensagens de validação (Data, Regra revogada, Vínculos, etc) e exibe de forma limpa
            ra.addFlashAttribute("erro", e.getMessage());
            
        } catch (Exception e) {
            // Se for um erro técnico ou de falha do banco de dados, devolve uma mensagem mascarada
            ra.addFlashAttribute("erro", "Ocorreu um erro inesperado no banco de dados ao salvar a atividade. Verifique os dados e tente novamente.");
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
        // Envia apenas os documentos que contêm regras ativas para os filtros iniciais
        model.addAttribute("listaResolucoes", regrasService.listarResolucoesComRegras());
        model.addAttribute("listaPortarias", regrasService.listarPortariasComRegras());
    }

    // Este endpoint recebe os IDs pelo Javascript e devolve o que for compatível em JSON
    @GetMapping("/api/filtros-regras")
    @ResponseBody
    public Map<String, Object> getFiltrosRegras(
            @RequestParam(required = false) Integer resolucaoId,
            @RequestParam(required = false) Integer portariaId) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Mantém a lógica de filtros compatíveis entre documentos
        if (resolucaoId != null && portariaId == null) {
            response.put("portariasCompativeis", regrasService.listarPortariasCompativeis(resolucaoId).stream()
                .map(p -> Map.of("id", p.getIdPortaria(), "nome", "Portaria " + p.getNumero() + "/" + p.getAno())).toList());
        }
        if (portariaId != null && resolucaoId == null) {
            response.put("resolucoesCompativeis", regrasService.listarResolucoesCompativeis(portariaId).stream()
                .map(r -> Map.of("id", r.getIdResolucao(), "nome", "Resolução " + r.getNumero() + "/" + r.getAno())).toList());
        }
        
        // NOVO: Retorna dados detalhados para o Guia de Verificação
        List<Regras> regras = regrasService.listarRegrasExatas(resolucaoId, portariaId);
        response.put("regras", regras.stream()
            .map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", r.getIdRegra());
                map.put("nome", r.getNomeRegra());
                map.put("descricao", r.getDescricao());
                map.put("pontos", r.getPontos());
                return map;
            }).toList());
            
        return response;
    }

    @GetMapping("/api/conselheiros-por-gestao")
    @ResponseBody
    public List<Map<String, Object>> getConselheirosPorGestao(@RequestParam Integer gestaoId) {
        // Busca os vínculos da gestão e mapeia para uma lista simples de ID e Nome
        return gestaoConselheiroRepository.findByIdIdGestao(gestaoId).stream()
                .map(gc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", gc.getConselheiro().getIdPessoa());
                    map.put("nome", gc.getConselheiro().getPessoa().getNome());
                    return map;
                }).toList();
    }

}