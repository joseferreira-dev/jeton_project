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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/conselheiro")
@PreAuthorize("hasRole('CONSELHEIRO')")
public class ConselheiroPortalController {

    private final AtividadeConselhalService atividadeService;
    private final JetonService jetonService;
    private final GestaoConselheiroService gestaoConselheiroService;
    private final ConselheiroService conselheiroService;
    private final TipoAnexoService tipoAnexoService;
    private final PontosSaldoService pontosSaldoService;

    ConselheiroPortalController(AtividadeConselhalService atividadeService, JetonService jetonService,
            GestaoConselheiroService gestaoConselheiroService, ConselheiroService conselheiroService,
            TipoAnexoService tipoAnexoService, PontosSaldoService pontosSaldoService) {
        this.atividadeService = atividadeService;
        this.jetonService = jetonService;
        this.gestaoConselheiroService = gestaoConselheiroService;
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
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) String situacao,
            Model model,
            HttpSession session) {

        if (!isConselheiro(session))
            return "redirect:/login";

        Integer idConselheiro = getIdConselheiroLogado(session);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dataHoraAtividade"));
        Page<AtividadeConselhal> pagina = atividadeService.listarPorConselheiroComFiltros(
                idConselheiro, dataInicio, dataFim, situacao, pageable);

        model.addAttribute("paginaAtividades", pagina);
        return "conselheiro/atividades";
    }

    @GetMapping("/atividades/nova")
    public String novaAtividade(Model model, HttpSession session, RedirectAttributes ra) {
        if (!isConselheiro(session))
            return "redirect:/login";

        Integer idConselheiro = getIdConselheiroLogado(session);
        Optional<Gestao> gestaoOpt = getGestaoAtivaDoConselheiro(idConselheiro);

        if (gestaoOpt.isEmpty()) {
            ra.addFlashAttribute("erro", "Você não possui uma gestão ativa. Não é possível criar novas atividades.");
            return "redirect:/conselheiro/dashboard";
        }

        Gestao gestao = gestaoOpt.get();
        AtividadeConselhal atividade = new AtividadeConselhal();
        Conselheiro conselheiro = conselheiroService.buscarPorId(idConselheiro)
                .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado"));
        atividade.setConselheiro(conselheiro);
        atividade.setGestao(gestao);
        atividade.setDataHoraRegistro(LocalDateTime.now());

        model.addAttribute("atividade", atividade);
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

        model.addAttribute("atividade", atividade);
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
        return "conselheiro/atividade_form";
    }

    @PostMapping("/atividades/salvar")
    public String salvarAtividade(@ModelAttribute("atividade") AtividadeConselhal atividade,
            @RequestParam("dataAtividadePura") String dataAtividadePura,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "idTipoAnexo", required = false) Integer idTipoAnexo,
            @RequestParam(value = "nomeComprovanteUsuario", required = false) String nomeComprovanteUsuario,
            HttpSession session,
            RedirectAttributes ra) {
        if (!isConselheiro(session))
            return "redirect:/login";

        if (atividade.getConselheiro() == null
                || !atividade.getConselheiro().getIdPessoa().equals(getIdConselheiroLogado(session))) {
            throw new RuntimeException("Conselheiro inválido para esta atividade.");
        }

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

    @GetMapping("/atividades/excluir/{id}")
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

    @GetMapping("/pagamentos")
    public String listarPagamentos(Model model, HttpSession session) {
        if (!isConselheiro(session))
            return "redirect:/login";

        Integer idConselheiro = getIdConselheiroLogado(session);
        List<JetonDTO> pagamentos = jetonService.listarPorConselheiro(idConselheiro);
        model.addAttribute("pagamentos", pagamentos);
        return "conselheiro/pagamentos";
    }

    @GetMapping("/perfil")
    public String perfil() {
        return "redirect:/usuarios/perfil";
    }
}