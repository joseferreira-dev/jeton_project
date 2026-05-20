package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.dominio.Comprovante;
import br.com.cremepe.jeton.servico.AtividadeConselhalService;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.RegrasService;
import br.com.cremepe.jeton.servico.ComprovanteService;
import br.com.cremepe.jeton.servico.TipoAnexoService;
import br.com.cremepe.jeton.repositorio.GestaoConselheiroRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/atividades")
public class AtividadeConselhalController {

    @Autowired
    private AtividadeConselhalService atividadeService;
    @Autowired
    private ConselheiroService conselheiroService;
    @Autowired
    private GestaoService gestaoService;
    @Autowired
    private RegrasService regrasService;
    @Autowired
    private GestaoConselheiroRepository gestaoConselheiroRepository;
    @Autowired
    private ComprovanteService comprovanteService;
    @Autowired
    private TipoAnexoService tipoAnexoService;

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

        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";

        Page<AtividadeConselhal> pagina = atividadeService.listarComPaginacaoEPesquisa(termo, situacao, turno, page,
                size, sort, dir);

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
        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";
        model.addAttribute("atividade", new AtividadeConselhal());
        carregarListasDeApoio(model);
        return "atividadeconselhal/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";
        AtividadeConselhal atividade = atividadeService.buscarPorId(id).orElseThrow();
        model.addAttribute("atividade", atividade);
        carregarListasDeApoio(model);
        return "atividadeconselhal/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("atividade") AtividadeConselhal atividade,
            @RequestParam("dataAtividadePura") String dataAtividadePura,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "idTipoAnexo", required = false) Integer idTipoAnexo,
            @RequestParam(value = "nomeComprovanteUsuario", required = false) String nomeComprovanteUsuario,
            RedirectAttributes ra) {
        try {
            if (dataAtividadePura == null || dataAtividadePura.trim().isEmpty()) {
                ra.addFlashAttribute("erro", "A data e o horário da atividade são obrigatórios para o enquadramento.");
                return "redirect:/atividades/novo";
            }

            // 1. Converte a string YYYY-MM-DDTHH:mm vinda do HTML diretamente para
            // LocalDateTime
            // Respeitando o horário exato selecionado manualmente pelo usuário no
            // formulário
            LocalDateTime dataHoraSelecionada = LocalDateTime.parse(dataAtividadePura);
            atividade.setDataHoraAtividade(dataHoraSelecionada);

            // 2. Se foi enviado um ficheiro, processamos o comprovante primeiro
            if (file != null && !file.isEmpty()) {
                if (idTipoAnexo == null || nomeComprovanteUsuario == null || nomeComprovanteUsuario.isEmpty()) {
                    throw new RuntimeException("O tipo e o nome do comprovante são obrigatórios.");
                }

                Comprovante comprovante = comprovanteService.guardarComprovante(file,
                        idTipoAnexo, nomeComprovanteUsuario);

                atividade.setComprovante(comprovante);
            }

            // O turno ('M', 'T', 'N') capturado pelo formulário continuará sendo repassado
            // para a service
            // permitindo que a validação de teto por período funcione com base no horário
            // real inserido.
            atividadeService.salvarAtividade(atividade);
            ra.addFlashAttribute("sucesso", "Atividade guardada com sucesso!");

        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao salvar: " + e.getMessage());
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
        model.addAttribute("listaResolucoes", regrasService.listarResolucoesComRegras());
        model.addAttribute("listaPortarias", regrasService.listarPortariasComRegras());
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
    }

    @GetMapping("/api/filtros-regras")
    @ResponseBody
    public Map<String, Object> getFiltrosRegras(
            @RequestParam(required = false) Integer resolucaoId,
            @RequestParam(required = false) Integer portariaId) {

        Map<String, Object> response = new HashMap<>();

        if (resolucaoId != null && portariaId == null) {
            response.put("portariasCompativeis", regrasService.listarPortariasCompativeis(resolucaoId).stream()
                    .map(p -> Map.of("id", p.getIdPortaria(), "nome", "Portaria " + p.getNumero() + "/" + p.getAno()))
                    .toList());
        }
        if (portariaId != null && resolucaoId == null) {
            response.put("resolucoesCompativeis", regrasService.listarResolucoesCompativeis(portariaId).stream()
                    .map(r -> Map.of("id", r.getIdResolucao(), "nome", "Resolução " + r.getNumero() + "/" + r.getAno()))
                    .toList());
        }

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
        return gestaoConselheiroRepository.findByIdIdGestao(gestaoId).stream()
                .map(gc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", gc.getConselheiro().getIdPessoa());
                    map.put("nome", gc.getConselheiro().getPessoa().getNome());
                    return map;
                }).toList();
    }

    @GetMapping("/api/regras-por-data")
    @ResponseBody
    public Map<String, Object> getRegrasENormativasPorData(@RequestParam String data) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Se vier o formato completo de datetime (com 'T'), limpa para pegar apenas a
            // data pura (10 caracteres)
            String dataFormatada = data.contains("T") ? data.split("T")[0] : data;
            LocalDate dataAtividade = LocalDate.parse(dataFormatada);

            Optional<Resolucao> optResolucao = regrasService.buscarResolucaoPorData(dataAtividade);
            Optional<Portaria> optPortaria = regrasService.buscarPortariaPorData(dataAtividade);

            Integer idResolucao = optResolucao.map(Resolucao::getIdResolucao).orElse(null);
            Integer idPortaria = optPortaria.map(Portaria::getIdPortaria).orElse(null);

            response.put("idResolucao", idResolucao);
            response.put("nomeResolucao", optResolucao.map(res -> "Resolução " + res.getNumero() + "/" + res.getAno())
                    .orElse("Nenhuma encontrada"));

            response.put("idPortaria", idPortaria);
            response.put("nomePortaria", optPortaria.map(port -> "Portaria " + port.getNumero() + "/" + port.getAno())
                    .orElse("Nenhuma (Apenas Resolução)"));

            if (idResolucao != null) {
                List<Regras> listaRegras = regrasService.listarRegrasPorNormativasInclusiveRevogadas(idResolucao,
                        idPortaria);

                response.put("regras", listaRegras.stream()
                        .map(regra -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", regra.getIdRegra());
                            map.put("nome", regra.getNomeRegra());
                            map.put("descricao", regra.getDescricao());
                            map.put("pontos", regra.getPontos());
                            return map;
                        }).toList());
            } else {
                response.put("regras", List.of());
            }
        } catch (Exception e) {
            response.put("erro", "Formato de data inválido ou erro interno ao processar.");
            response.put("regras", List.of());
        }
        return response;
    }
}