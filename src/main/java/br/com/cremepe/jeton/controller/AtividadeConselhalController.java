package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.AtividadeConselhal;
import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.dto.LoteAtividadeDTO;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.GestaoConselheiroRepository;
import br.com.cremepe.jeton.service.AtividadeConselhalService;
import br.com.cremepe.jeton.service.ConselheiroService;
import br.com.cremepe.jeton.service.GestaoService;
import br.com.cremepe.jeton.service.RegrasService;
import br.com.cremepe.jeton.service.TipoAnexoService;
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
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/atividades")
public class AtividadeConselhalController {

    // =========================================================================
    // DEPENDÊNCIAS
    // =========================================================================
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
    private TipoAnexoService tipoAnexoService;
    @Autowired
    private AtividadeConselhalRepository atividadeRepository;

    // =========================================================================
    // PÁGINAS WEB (LISTAGEM, FORMULÁRIOS, AÇÕES)
    // =========================================================================

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "turno", required = false, defaultValue = "") String turno,
            @RequestParam(value = "comprovanteFiltro", required = false, defaultValue = "") String comprovanteFiltro,
            @RequestParam(value = "dataInicio", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(value = "dataFim", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "dataHoraAtividade") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "desc") String dir,
            Model model) {

        Page<AtividadeConselhal> pagina = atividadeService.listarComPaginacaoEPesquisa(
                termo, situacao, turno, comprovanteFiltro, dataInicio, dataFim, page, size, sort, dir);

        model.addAttribute("paginaAtividades", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("turno", turno);
        model.addAttribute("comprovanteFiltro", comprovanteFiltro);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("reverseSortDir", dir.equalsIgnoreCase("asc") ? "desc" : "asc");

        return "atividadeconselhal/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("atividade", new AtividadeConselhal());
        carregarListasDeApoio(model);
        return "atividadeconselhal/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session,
            RedirectAttributes ra) {
        if (naoAutenticado(session))
            return "redirect:/login";
        AtividadeConselhal atividade = atividadeService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Atividade não encontrada"));

        // Se o comprovante e este é compartilhado com outras atividades
        if (atividade.getComprovante() != null) {
            long count = atividadeRepository
                    .countByComprovanteIdComprovante(atividade.getComprovante().getIdComprovante());
            if (count > 1) {
                ra.addFlashAttribute("info",
                        "Esta atividade compartilha o mesmo comprovante com outras " + (count - 1) +
                                " atividades. Para editar todas de uma vez, utilize a edição em lote.");
                return "redirect:/atividades/lote/editar/" + atividade.getComprovante().getIdComprovante();
            }
        }

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
            HttpSession session,
            RedirectAttributes ra) {
        try {
            LocalDateTime dataHora = parseDataHora(dataAtividadePura, ra);
            if (dataHora == null)
                return "redirect:/atividades/novo";

            atividade.setDataHoraAtividade(dataHora);
            atividade.setInTurno(calcularTurno(dataHora.getHour()));

            if (atividade.getIdAtividade() == null) {
                atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
                atividadeService.criar(atividade, file, idTipoAnexo, nomeComprovanteUsuario);
                ra.addFlashAttribute("sucesso", "Atividade criada com sucesso!");
            } else {
                if (atividade.getInSituacao() == null || atividade.getInSituacao().trim().isEmpty()) {
                    atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
                }
                Integer idComprovanteAntigo = null;
                AtividadeConselhal atividadeBanco = atividadeService.buscarPorId(atividade.getIdAtividade())
                        .orElse(null);
                if (atividadeBanco != null && atividadeBanco.getComprovante() != null) {
                    idComprovanteAntigo = atividadeBanco.getComprovante().getIdComprovante();
                }
                atividadeService.atualizar(atividade, file, idTipoAnexo, nomeComprovanteUsuario,
                        idComprovanteAntigo);
                ra.addFlashAttribute("sucesso", "Atividade atualizada com sucesso!");
            }
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao salvar: " + e.getMessage());
        }
        return "redirect:/atividades";
    }

    @GetMapping("/validar/{id}")
    public String validar(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            atividadeService.validar(id);
            ra.addFlashAttribute("sucesso",
                    "Atividade validada com sucesso! Ela agora está apta a receber processamento financeiro.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro interno ao validar: " + e.getMessage());
        }
        return "redirect:/atividades";
    }

    @GetMapping("/desvalidar/{id}")
    public String desvalidar(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            atividadeService.desvalidar(id);
            ra.addFlashAttribute("sucesso", "Atividade retornada ao status Pendente.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/atividades";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            atividadeService.excluir(id);
            ra.addFlashAttribute("sucesso", "Atividade removida com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro interno ao remover atividade: " + e.getMessage());
        }
        return "redirect:/atividades";
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }

    private LocalDateTime parseDataHora(String dataAtividadePura, RedirectAttributes ra) {
        if (dataAtividadePura == null || dataAtividadePura.trim().isEmpty()) {
            ra.addFlashAttribute("erro", "A data e o horário da atividade são obrigatórios para o enquadramento.");
            return null;
        }
        try {
            return LocalDateTime.parse(dataAtividadePura);
        } catch (DateTimeParseException e) {
            ra.addFlashAttribute("erro", "Formato de data/hora inválido. Use 'YYYY-MM-DDTHH:mm'.");
            return null;
        }
    }

    private String calcularTurno(int hora) {
        if (hora >= 6 && hora < 12)
            return AtividadeConselhal.TURNO_MANHA;
        if (hora >= 12 && hora < 18)
            return AtividadeConselhal.TURNO_TARDE;
        return AtividadeConselhal.TURNO_NOITE;
    }

    private void carregarListasDeApoio(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("listaResolucoes", regrasService.listarResolucoesComRegras());
        model.addAttribute("listaPortarias", regrasService.listarPortariasComRegras());
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
    }

    // =========================================================================
    // ENDPOINTS DA API (RESPONSE BODY)
    // =========================================================================

    @GetMapping("/api/filtros-regras")
    @ResponseBody
    public Map<String, Object> getFiltrosRegras(
            @RequestParam(required = false) Integer resolucaoId,
            @RequestParam(required = false) Integer portariaId) {

        Map<String, Object> response = new HashMap<>();

        if (resolucaoId != null && portariaId == null) {
            response.put("portariasCompativeis", regrasService.listarPortariasCompativeis(resolucaoId).stream()
                    .map(p -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", p.getIdPortaria());
                        map.put("nome", "Portaria " + p.getNumero() + "/" + p.getAno());
                        return map;
                    })
                    .collect(Collectors.toList()));
        }
        if (portariaId != null && resolucaoId == null) {
            response.put("resolucoesCompativeis", regrasService.listarResolucoesCompativeis(portariaId).stream()
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", r.getIdResolucao());
                        map.put("nome", "Resolução " + r.getNumero() + "/" + r.getAno());
                        return map;
                    })
                    .collect(Collectors.toList()));
        }

        List<Regras> regras = regrasService.listarRegrasExatas(resolucaoId, portariaId);
        response.put("regras", regras.stream()
                .map(this::mapearRegra)
                .collect(Collectors.toList()));
        return response;
    }

    @GetMapping("/api/conselheiros-por-gestao")
    @ResponseBody
    public List<Map<String, Object>> getConselheirosPorGestao(@RequestParam Integer gestaoId) {
        return gestaoConselheiroRepository.findByIdIdGestao(gestaoId).stream()
                .sorted(Comparator.comparing(gc -> gc.getConselheiro().getPessoa().getNome()))
                .map(gc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", gc.getConselheiro().getIdPessoa());
                    map.put("nome", gc.getConselheiro().getPessoa().getNome());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/api/regras-por-data")
    @ResponseBody
    public Map<String, Object> getRegrasENormativasPorData(@RequestParam String data) {
        Map<String, Object> response = new HashMap<>();
        try {
            String dataFormatada = data.contains("T") ? data.split("T")[0] : data;
            LocalDate dataAtividade = LocalDate.parse(dataFormatada);

            Optional<Resolucao> optResolucao = regrasService.buscarResolucaoPorData(dataAtividade);
            Optional<Portaria> optPortaria = regrasService.buscarPortariaPorData(dataAtividade);

            Integer idResolucao = optResolucao.map(Resolucao::getIdResolucao).orElse(null);
            Integer idPortaria = optPortaria.map(Portaria::getIdPortaria).orElse(null);

            response.put("idResolucao", idResolucao);
            response.put("nomeResolucao", optResolucao.map(this::formatarResolucao).orElse("Nenhuma encontrada"));
            response.put("idPortaria", idPortaria);
            response.put("nomePortaria", optPortaria.map(this::formatarPortaria).orElse("Nenhuma (Apenas Resolução)"));

            if (idResolucao != null) {
                List<Regras> listaRegras = regrasService.listarRegrasPorNormativasInclusiveRevogadas(idResolucao,
                        idPortaria);
                response.put("regras", listaRegras.stream()
                        .sorted(Comparator.comparing(Regras::getNomeRegra))
                        .map(this::mapearRegra)
                        .collect(Collectors.toList()));
            } else {
                response.put("regras", Collections.emptyList());
            }
        } catch (Exception e) {
            response.put("erro", "Formato de data inválido ou erro interno ao processar.");
            response.put("regras", Collections.emptyList());
        }
        return response;
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE FORMATAÇÃO (API)
    // =========================================================================

    private String formatarPortaria(Portaria p) {
        return "Portaria " + p.getNumero() + "/" + p.getAno();
    }

    private String formatarResolucao(Resolucao r) {
        return "Resolução " + r.getNumero() + "/" + r.getAno();
    }

    private Map<String, Object> mapearRegra(Regras regra) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", regra.getIdRegra());
        map.put("nome", regra.getNomeRegra());
        map.put("descricao", regra.getDescricao());
        map.put("pontos", regra.getPontos());
        return map;
    }

    // =========================================================================
    // CRIAÇÃO EM LOTE
    // =========================================================================

    @GetMapping("/lote/novo")
    public String prepararLote(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
        return "atividadeconselhal/lote_formulario";
    }

    @PostMapping("/lote/salvar")
    public String salvarLote(@ModelAttribute LoteAtividadeDTO dto,
            HttpSession session,
            RedirectAttributes ra) {
        if (naoAutenticado(session))
            return "redirect:/login";
        try {
            // Se o turno não foi enviado, ele será calculado dentro do service
            if (dto.getInTurno() == null || dto.getInTurno().isEmpty()) {
                dto.setInTurno(null);
            }
            atividadeService.criarLote(dto);
            ra.addFlashAttribute("sucesso",
                    "Atividades criadas em lote com sucesso para " + dto.getIdsConselheiros().size()
                            + " conselheiros.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao criar lote: " + e.getMessage());
        }
        return "redirect:/atividades";
    }

    @GetMapping("/lote/editar/{idComprovante}")
    public String prepararEdicaoLote(@PathVariable Integer idComprovante, Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        List<AtividadeConselhal> atividades = atividadeService.listarPorComprovante(idComprovante);
        if (atividades.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma atividade encontrada para este comprovante.");
        }
        AtividadeConselhal referencia = atividades.get(0);

        // IDs dos conselheiros atualmente no lote
        List<Integer> idsConselheirosAtuais = atividades.stream()
                .map(a -> a.getConselheiro().getIdPessoa())
                .distinct()
                .collect(Collectors.toList());

        model.addAttribute("atividadeReferencia", referencia);
        model.addAttribute("quantidade", atividades.size());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
        model.addAttribute("idComprovante", idComprovante);
        model.addAttribute("idsConselheirosAtuais", idsConselheirosAtuais);
        return "atividadeconselhal/lote_edicao";
    }

    @PostMapping("/lote/atualizar/{idComprovante}")
    public String atualizarLote(@PathVariable Integer idComprovante,
            @ModelAttribute LoteAtividadeDTO dto,
            HttpSession session,
            RedirectAttributes ra) {
        if (naoAutenticado(session))
            return "redirect:/login";
        try {
            if (dto.getInTurno() == null || dto.getInTurno().isEmpty()) {
                dto.setInTurno(null);
            }
            atividadeService.atualizarLote(idComprovante, dto);
            ra.addFlashAttribute("sucesso", "Todas as atividades vinculadas ao comprovante foram atualizadas.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao atualizar lote: " + e.getMessage());
        }
        return "redirect:/atividades";
    }
}