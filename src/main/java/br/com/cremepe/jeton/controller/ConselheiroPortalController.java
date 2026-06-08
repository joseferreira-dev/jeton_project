package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.JetonRepository;
import br.com.cremepe.jeton.repository.PontosSaldoRepository;
import br.com.cremepe.jeton.servico.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/conselheiro")
public class ConselheiroPortalController {

    @Autowired
    private AtividadeConselhalService atividadeService;
    @Autowired
    private JetonService jetonService;
    @Autowired
    private GestaoConselheiroService gestaoConselheiroService;
    @Autowired
    private ConselheiroService conselheiroService;
    @Autowired
    private TipoAnexoService tipoAnexoService;
    @Autowired
    private PontosSaldoRepository pontosSaldoRepository;
    @Autowired
    private AtividadeConselhalRepository atividadeRepository;
    @Autowired
    private JetonRepository jetonRepository;

    // =========================================================================
    // VERIFICAÇÕES DE ACESSO E UTILITÁRIOS
    // =========================================================================

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

    // =========================================================================
    // DASHBOARD
    // =========================================================================

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

        // Saldo total de pontos (saldo antigo + atividades validadas não computadas)
        Integer pontosSaldoAntigo = pontosSaldoRepository.somarPontosSobrandoTotal(idConselheiro);
        if (pontosSaldoAntigo == null)
            pontosSaldoAntigo = 0;
        Integer pontosAtividadesNaoComputadas = atividadeRepository
                .sumPontosAtividadesValidadasNaoComputadas(idConselheiro);
        if (pontosAtividadesNaoComputadas == null)
            pontosAtividadesNaoComputadas = 0;
        int saldoTotal = pontosSaldoAntigo + pontosAtividadesNaoComputadas;

        // Novos indicadores
        long atividadesPendentes = atividadeRepository.countByConselheiroIdPessoaAndInSituacao(idConselheiro, "P");
        long atividadesTotais = atividadeRepository.countByConselheiroIdPessoa(idConselheiro);
        BigDecimal totalRecebido = jetonRepository.sumValorRecebidoPorConselheiro(idConselheiro);
        if (totalRecebido == null)
            totalRecebido = BigDecimal.ZERO;

        // Gestão ativa (apenas para exibição)
        Optional<Gestao> gestaoAtivaOpt = getGestaoAtivaDoConselheiro(idConselheiro);
        String nomeGestaoAtiva = gestaoAtivaOpt.map(Gestao::getNomeGestao).orElse(null);

        // Últimas atividades (lista)
        Pageable top5 = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dataHoraAtividade"));
        Page<AtividadeConselhal> paginaAtividades = atividadeService.listarPorConselheiro(idConselheiro, top5);
        List<Jeton> ultimosPagamentos = jetonService.listarPorConselheiro(idConselheiro, 5);

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

    // =========================================================================
    // ATIVIDADES (CRUD restrito ao próprio conselheiro)
    // =========================================================================

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

        Gestao gestao = gestaoOpt.get(); // <-- Extrai a Gestão do Optional

        // Restante do método (criação da atividade, etc.)
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

        try {
            // Validação: garantir que o conselheiro não está tentando associar outra pessoa
            if (atividade.getConselheiro() == null
                    || !atividade.getConselheiro().getIdPessoa().equals(getIdConselheiroLogado(session))) {
                throw new RuntimeException("Conselheiro inválido para esta atividade.");
            }

            LocalDateTime dataHora = parseDataHora(dataAtividadePura, ra);
            if (dataHora == null)
                return "redirect:/conselheiro/atividades/nova";

            atividade.setDataHoraAtividade(dataHora);
            atividade.setInTurno(calcularTurno(dataHora.getHour()));

            // Se for nova, define situação pendente
            if (atividade.getIdAtividade() == null) {
                atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
            }

            if (atividade.getIdAtividade() == null) {
                // Criação
                atividadeService.criar(atividade, file, idTipoAnexo, nomeComprovanteUsuario);
            } else {
                // Edição
                Integer idComprovanteAntigo = null;
                AtividadeConselhal existente = atividadeService.buscarPorId(atividade.getIdAtividade()).orElse(null);
                if (existente != null && existente.getComprovante() != null) {
                    idComprovanteAntigo = existente.getComprovante().getIdComprovante();
                }
                atividadeService.atualizar(atividade, file, idTipoAnexo, nomeComprovanteUsuario,
                        idComprovanteAntigo);
            }
            ra.addFlashAttribute("sucesso", "Atividade salva com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar atividade: " + e.getMessage());
        }
        return "redirect:/conselheiro/atividades";
    }

    @GetMapping("/atividades/excluir/{id}")
    public String excluirAtividade(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        if (!isConselheiro(session))
            return "redirect:/login";

        try {
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
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/conselheiro/atividades";
    }

    // =========================================================================
    // PAGAMENTOS (JETONS DO CONSELHEIRO)
    // =========================================================================

    @GetMapping("/pagamentos")
    public String listarPagamentos(Model model, HttpSession session) {
        if (!isConselheiro(session))
            return "redirect:/login";

        Integer idConselheiro = getIdConselheiroLogado(session);
        List<Jeton> pagamentos = jetonService.listarPorConselheiro(idConselheiro);
        model.addAttribute("pagamentos", pagamentos);
        return "conselheiro/pagamentos";
    }

    // =========================================================================
    // PERFIL (redireciona para a tela existente)
    // =========================================================================

    @GetMapping("/perfil")
    public String perfil() {
        return "redirect:/usuarios/perfil";
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    private LocalDateTime parseDataHora(String dataAtividadePura, RedirectAttributes ra) {
        if (dataAtividadePura == null || dataAtividadePura.trim().isEmpty()) {
            ra.addFlashAttribute("erro", "A data e horário da atividade são obrigatórios.");
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
            return "M";
        if (hora >= 12 && hora < 18)
            return "T";
        return "N";
    }
}