package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.dto.JetonDTO;
import br.com.cremepe.jeton.service.*;
import br.com.cremepe.jeton.util.DataUtils;
import br.com.cremepe.jeton.util.TurnoUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/conselheiro")
@PreAuthorize("hasRole('CONSELHEIRO')")
public class ConselheiroPortalController {

    private final AtividadeConselhalService atividadeService;
    private final JetonService jetonService;
    private final GestaoConselheiroService gestaoConselheiroService;
    private final GestaoService gestaoService;
    private final ConselheiroService conselheiroService;
    private final TipoAnexoService tipoAnexoService;
    private final PontosSaldoService pontosSaldoService;

    ConselheiroPortalController(
            AtividadeConselhalService atividadeService,
            JetonService jetonService,
            GestaoConselheiroService gestaoConselheiroService,
            GestaoService gestaoService,
            ConselheiroService conselheiroService,
            TipoAnexoService tipoAnexoService,
            PontosSaldoService pontosSaldoService) {
        this.atividadeService = atividadeService;
        this.jetonService = jetonService;
        this.gestaoConselheiroService = gestaoConselheiroService;
        this.gestaoService = gestaoService;
        this.conselheiroService = conselheiroService;
        this.tipoAnexoService = tipoAnexoService;
        this.pontosSaldoService = pontosSaldoService;
    }

    private boolean isConselheiro(HttpSession session) {
        ViewUserLogin user = (ViewUserLogin) session.getAttribute("usuarioLogado");
        return user != null && "C".equals(user.getInTipoPessoa());
    }

    private Integer getIdConselheiroLogado(HttpSession session) {
        ViewUserLogin user = (ViewUserLogin) session.getAttribute("usuarioLogado");
        return user != null ? user.getIdPessoa() : null;
    }

    private Optional<Gestao> getGestaoAtivaDoConselheiro(Integer idConselheiro) {
        return gestaoConselheiroService.buscarPorConselheiroEStatus(idConselheiro, GestaoConselheiro.SITUACAO_ATIVO)
                .map(GestaoConselheiro::getGestao);
    }

    @GetMapping
    public String redirectToDashboard() {
        return "redirect:/conselheiro/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        if (!isConselheiro(session))
            return "redirect:/login";

        Integer idConselheiro = getIdConselheiroLogado(session);
        boolean hasAnyVinculo = gestaoConselheiroService.existeVinculoParaConselheiro(idConselheiro);

        if (!hasAnyVinculo) {
            model.addAttribute("nuncaVinculado", true);
            model.addAttribute("mensagem",
                    "Você ainda não foi vinculado a nenhuma gestão. Entre em contato com o administrador.");
            return "conselheiro/dashboard";
        }

        int saldoTotal = pontosSaldoService.somarPontosSobrandoTotal(idConselheiro)
                + atividadeService.sumPontosValidadasNaoComputadas(idConselheiro);
        long atividadesPendentes = atividadeService.countPendentesPorConselheiro(idConselheiro);
        long atividadesTotais = atividadeService.countTotalPorConselheiro(idConselheiro);
        BigDecimal totalRecebido = jetonService.sumValorRecebidoPorConselheiro(idConselheiro);
        if (totalRecebido == null)
            totalRecebido = BigDecimal.ZERO;

        Optional<Gestao> gestaoAtivaOpt = getGestaoAtivaDoConselheiro(idConselheiro);
        String nomeGestaoAtiva = gestaoAtivaOpt.map(Gestao::getNomeGestao).orElse(null);

        Pageable top5 = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dataHoraAtividade"));
        Page<AtividadeConselhal> paginaAtividades = atividadeService.listarPorConselheiro(idConselheiro, top5);
        List<JetonDTO> ultimosPagamentos = jetonService.listarPorConselheiro(idConselheiro, 5);

        model.addAttribute("nuncaVinculado", false);
        model.addAttribute("temGestaoAtiva", gestaoAtivaOpt.isPresent());
        model.addAttribute("gestaoAtual", nomeGestaoAtiva != null ? nomeGestaoAtiva : "Nenhuma gestão ativa");
        model.addAttribute("saldoTotalPontos", saldoTotal);
        model.addAttribute("atividadesPendentes", atividadesPendentes);
        model.addAttribute("atividadesTotais", atividadesTotais);
        model.addAttribute("totalRecebido", totalRecebido);
        model.addAttribute("ultimasAtividades", paginaAtividades.getContent());
        model.addAttribute("ultimosPagamentos", ultimosPagamentos);

        return "conselheiro/dashboard";
    }

    @GetMapping("/atividades")
    public String listarAtividades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false) String turno,
            @RequestParam(required = false) String comprovanteFiltro,
            @RequestParam(required = false) String termo,
            @RequestParam(required = false, defaultValue = "dataHoraAtividade") String sort,
            @RequestParam(required = false, defaultValue = "desc") String dir,
            Model model,
            HttpSession session) {

        if (!isConselheiro(session))
            return "redirect:/login";

        Integer idConselheiro = getIdConselheiroLogado(session);

        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(dir), sort));
        Page<AtividadeConselhal> pagina = atividadeService.listarPorConselheiroComFiltros(
                idConselheiro, inicio, fim, situacao, turno, comprovanteFiltro, termo, pageable);

        model.addAttribute("paginaAtividades", pagina);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("situacao", situacao);
        model.addAttribute("turno", turno);
        model.addAttribute("comprovanteFiltro", comprovanteFiltro);
        model.addAttribute("termo", termo);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("size", size);

        return "conselheiro/atividades";
    }

    @GetMapping("/atividades/nova")
    public String novaAtividade(Model model, HttpSession session, RedirectAttributes ra) {
        if (!isConselheiro(session))
            return "redirect:/login";

        Integer idConselheiro = getIdConselheiroLogado(session);
        List<Gestao> gestoesAtivas = getGestoesAtivasDoConselheiro(idConselheiro);

        if (gestoesAtivas.isEmpty()) {
            ra.addFlashAttribute("erro",
                    "Você não possui nenhuma gestão ativa. Não é possível criar novas atividades.");
            return "redirect:/conselheiro/dashboard";
        }

        AtividadeConselhal atividade = new AtividadeConselhal();
        Conselheiro conselheiro = conselheiroService.buscarPorId(idConselheiro)
                .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado"));
        atividade.setConselheiro(conselheiro);
        atividade.setDataHoraRegistro(LocalDateTime.now());

        if (gestoesAtivas.size() == 1) {
            atividade.setGestao(gestoesAtivas.get(0));
        }

        model.addAttribute("atividade", atividade);
        model.addAttribute("listaGestoesAtivas", gestoesAtivas);
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
        return "conselheiro/atividade_form";
    }

    @GetMapping("/atividades/editar/{id}")
    public String editarAtividade(@PathVariable Integer id, Model model, HttpSession session, RedirectAttributes ra) {
        if (!isConselheiro(session))
            return "redirect:/login";

        Integer idConselheiro = getIdConselheiroLogado(session);
        AtividadeConselhal atividade = atividadeService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Atividade não encontrada"));

        if (!atividade.getConselheiro().getIdPessoa().equals(idConselheiro)) {
            ra.addFlashAttribute("erro", "Você só pode editar suas próprias atividades.");
            return "redirect:/conselheiro/atividades";
        }
        if (!atividade.isPendente()) {
            ra.addFlashAttribute("erro", "Apenas atividades pendentes podem ser editadas.");
            return "redirect:/conselheiro/atividades";
        }

        List<Gestao> gestoesAtivas = getGestoesAtivasDoConselheiro(idConselheiro);
        if (gestoesAtivas.isEmpty()) {
            ra.addFlashAttribute("erro", "Você não possui gestão ativa. Não é possível editar.");
            return "redirect:/conselheiro/atividades";
        }

        model.addAttribute("atividade", atividade);
        model.addAttribute("listaGestoesAtivas", gestoesAtivas);
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
        return "conselheiro/atividade_form";
    }

    @PostMapping("/atividades/salvar")
    public String salvarAtividade(@ModelAttribute("atividade") AtividadeConselhal atividade,
            @RequestParam("dataAtividadePura") String dataAtividadePura,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "idTipoAnexo", required = false) Integer idTipoAnexo,
            @RequestParam(value = "nomeComprovanteUsuario", required = false) String nomeComprovanteUsuario,
            @RequestParam("idGestao") Integer idGestao,
            HttpSession session,
            RedirectAttributes ra) {
        if (!isConselheiro(session))
            return "redirect:/login";

        Integer idConselheiro = getIdConselheiroLogado(session);
        if (atividade.getConselheiro() == null || !atividade.getConselheiro().getIdPessoa().equals(idConselheiro)) {
            throw new RuntimeException("Conselheiro inválido para esta atividade.");
        }

        List<Gestao> gestoesAtivas = getGestoesAtivasDoConselheiro(idConselheiro);
        boolean gestaoValida = gestoesAtivas.stream().anyMatch(g -> g.getIdGestao().equals(idGestao));
        if (!gestaoValida) {
            ra.addFlashAttribute("erro", "A gestão selecionada não está ativa para você.");
            return "redirect:/conselheiro/atividades/nova";
        }

        Gestao gestao = gestaoService.buscarGestaoOuFalhar(idGestao);
        atividade.setGestao(gestao);

        LocalDateTime dataHora = DataUtils.parseDataHora(dataAtividadePura,
                "A data e horário da atividade são obrigatórios.");
        atividade.setDataHoraAtividade(dataHora);
        atividade.setInTurno(TurnoUtils.calcularTurno(dataHora.getHour()));

        if (atividade.getIdAtividade() == null) {
            atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
            atividadeService.criar(atividade, file, idTipoAnexo, nomeComprovanteUsuario);
        } else {
            Integer idComprovanteAntigo = null;
            AtividadeConselhal existente = atividadeService.buscarPorId(atividade.getIdAtividade()).orElse(null);
            if (existente != null && existente.getComprovante() != null) {
                idComprovanteAntigo = existente.getComprovante().getIdComprovante();
            }
            atividadeService.atualizar(atividade, file, idTipoAnexo, nomeComprovanteUsuario, idComprovanteAntigo);
        }
        ra.addFlashAttribute("sucesso", "Atividade salva com sucesso!");
        return "redirect:/conselheiro/atividades";
    }

    @PostMapping("/atividades/excluir/{id}")
    public String excluirAtividade(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        if (!isConselheiro(session))
            return "redirect:/login";

        AtividadeConselhal atividade = atividadeService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada"));
        if (!atividade.getConselheiro().getIdPessoa().equals(getIdConselheiroLogado(session))) {
            throw new RuntimeException("Você só pode excluir suas próprias atividades.");
        }
        if (!atividade.isPendente()) {
            throw new RuntimeException("Apenas atividades pendentes podem ser excluídas.");
        }
        atividadeService.excluir(id);
        ra.addFlashAttribute("sucesso", "Atividade excluída com sucesso.");
        return "redirect:/conselheiro/atividades";
    }

    @GetMapping("/perfil")
    public String perfil() {
        return "redirect:/usuarios/perfil";
    }

    @GetMapping("/pagamentos")
    public String listarPagamentos(
            @RequestParam(required = false) Integer idGestao,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ano") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            Model model,
            HttpSession session) {
        if (!isConselheiro(session))
            return "redirect:/login";
        Integer idConselheiro = getIdConselheiroLogado(session);

        Map<String, String> sortMapping = new HashMap<>();
        sortMapping.put("nomeConselheiro", "conselheiro.pessoa.nome");
        sortMapping.put("nomeGestao", "gestao.nomeGestao");
        sortMapping.put("totalJeton", "totalJeton");
        sortMapping.put("valor", "valor");
        sortMapping.put("mes", "mes");
        sortMapping.put("ano", "ano");
        sortMapping.put("situacao", "inSituacao");
        String sortField = sortMapping.getOrDefault(sort, sort);
        Sort.Direction direction = Sort.Direction.fromString(dir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<JetonDTO> pagina = jetonService.listarPorConselheiroPaginado(idConselheiro, idGestao, mes, ano, pageable);
        model.addAttribute("paginaPagamentos", pagina);
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("idGestaoSelecionada", idGestao);
        model.addAttribute("mesSelecionado", mes);
        model.addAttribute("anoSelecionado", ano);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "conselheiro/pagamentos";
    }

    private List<Gestao> getGestoesAtivasDoConselheiro(Integer idConselheiro) {
        return gestaoConselheiroService.listarPorConselheiroAtivos(idConselheiro)
                .stream()
                .map(GestaoConselheiro::getGestao)
                .collect(Collectors.toList());
    }

}