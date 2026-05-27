package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dominio.Jeton;
import br.com.cremepe.jeton.dominio.PontosSaldo;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.repositorio.JetonRepository;
import br.com.cremepe.jeton.repositorio.PontosSaldoRepository;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.JetonService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/jeton")
public class JetonController {

    @Autowired
    private JetonService jetonService;
    @Autowired
    private GestaoService gestaoService;
    @Autowired
    private ConselheiroService conselheiroService;
    @Autowired
    private JetonRepository jetonRepository;
    @Autowired
    private PontosSaldoRepository pontosSaldoRepository;

    // =========================================================================
    // LISTAGEM (tela principal)
    // =========================================================================
    @GetMapping
    public String listar(
            @RequestParam(value = "idGestao", required = false) Integer idGestao,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano,
            Model model, HttpSession session) {

        if (naoAutenticado(session))
            return "redirect:/login";

        LocalDate hoje = LocalDate.now();
        Integer mesFiltro = (mes != null) ? mes : hoje.getMonthValue();
        Integer anoFiltro = (ano != null) ? ano : hoje.getYear();

        List<Jeton> listaBruta = new ArrayList<>();
        if (idGestao != null) {
            listaBruta = jetonService.listarTodos().stream()
                    .filter(j -> j.getGestao().getIdGestao().equals(idGestao) &&
                            j.getMes().equals(mesFiltro) &&
                            j.getAno().equals(anoFiltro))
                    .toList();
            model.addAttribute("idGestaoSelecionada", idGestao);
        }

        // Agrupamento visual por conselheiro
        Map<Integer, Jeton> agrupado = new LinkedHashMap<>();
        for (Jeton j : listaBruta) {
            Integer idPessoa = j.getConselheiro().getIdPessoa();
            if (agrupado.containsKey(idPessoa)) {
                Jeton existente = agrupado.get(idPessoa);
                existente.setTotalJeton(existente.getTotalJeton() + j.getTotalJeton());
                existente.setValor(existente.getValor().add(j.getValor()));
            } else {
                Jeton clone = new Jeton();
                clone.setConselheiro(j.getConselheiro());
                clone.setGestao(j.getGestao());
                clone.setMes(j.getMes());
                clone.setAno(j.getAno());
                clone.setTotalJeton(j.getTotalJeton());
                clone.setValor(j.getValor());
                clone.setInSituacao(j.getInSituacao());
                agrupado.put(idPessoa, clone);
            }
        }

        model.addAttribute("listaJetons", agrupado.values());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("mesAtual", mesFiltro);
        model.addAttribute("anoAtual", anoFiltro);
        return "jeton/lista";
    }

    // =========================================================================
    // PROCESSAR FOLHA
    // =========================================================================
    @PostMapping("/processar")
    public String processar(@RequestParam("idGestao") Integer idGestao,
            @RequestParam("mes") Integer mes,
            @RequestParam("ano") Integer ano,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Optional<Gestao> gestaoOpt = gestaoService.buscarPorId(idGestao);
            if (gestaoOpt.isEmpty()) {
                throw new RuntimeException("Gestão não encontrada.");
            }
            Integer idUsuario = getIdUsuarioLogado(session);
            jetonService.processarFechamentoMensal(gestaoOpt.get(), mes, ano, idUsuario);
            ra.addFlashAttribute("sucesso", "Cálculo da folha mensal executado com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro inesperado ao processar: " + e.getMessage());
        }
        return "redirect:/jeton?idGestao=" + idGestao + "&mes=" + mes + "&ano=" + ano;
    }

    // =========================================================================
    // FECHAR DEFINITIVO (HOMOLOGAR)
    // =========================================================================
    @PostMapping("/fechar-definitivo")
    public String fecharDefinitivo(@RequestParam("idGestao") Integer idGestao,
            @RequestParam("mes") Integer mes,
            @RequestParam("ano") Integer ano,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Optional<Gestao> gestaoOpt = gestaoService.buscarPorId(idGestao);
            if (gestaoOpt.isEmpty()) {
                throw new RuntimeException("Gestão não encontrada.");
            }
            Integer idUsuario = getIdUsuarioLogado(session);
            jetonService.realizarFechamentoDefinitivoFolha(gestaoOpt.get(), mes, ano, idUsuario);
            ra.addFlashAttribute("sucesso", "Folha fechada e homologada definitivamente para " + mes + "/" + ano);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro interno ao homologar o fechamento: " + e.getMessage());
        }
        return "redirect:/jeton";
    }

    // =========================================================================
    // ESTORNAR UM JETON ESPECÍFICO
    // =========================================================================
    @GetMapping("/estornar/{id}")
    public String estornarJeton(@PathVariable("id") Integer idJeton,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            Integer idUsuario = getIdUsuarioLogado(session);
            jetonService.estornarJetonPontual(idJeton, idUsuario);
            ra.addFlashAttribute("sucesso", "Jeton estornado com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao estornar Jeton: " + e.getMessage());
        }
        return "redirect:/jeton";
    }

    // =========================================================================
    // HISTÓRICO (JÁ FECHADOS)
    // =========================================================================
    @GetMapping("/historico")
    public String exibirHistorico(
            @RequestParam(value = "idGestao", required = false) Integer idGestao,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano,
            @RequestParam(value = "termo", required = false) String termo,
            Model model, HttpSession session) {

        if (naoAutenticado(session))
            return "redirect:/login";

        List<Jeton> historico = jetonService.pesquisarHistorico(idGestao, mes, ano, termo);
        model.addAttribute("listaJetons", historico);
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("idGestaoSelecionada", idGestao);
        model.addAttribute("mesSelecionado", mes);
        model.addAttribute("anoSelecionado", ano);
        model.addAttribute("termo", termo);
        return "jeton/historico";
    }

    // =========================================================================
    // API: ATIVIDADES VINCULADAS A UM JETON (para modal)
    // =========================================================================
    @GetMapping("/atividades/conselheiro/{idPessoa}/gestao/{idGestao}/mes/{mes}/ano/{ano}")
    @ResponseBody
    public List<Map<String, Object>> obterAtividadesVinculadas(
            @PathVariable("idPessoa") Integer idPessoa,
            @PathVariable("idGestao") Integer idGestao,
            @PathVariable("mes") Integer mes,
            @PathVariable("ano") Integer ano) {
        return jetonService.listarAtividadesAgrupadasPorConselheiro(idPessoa, idGestao, mes, ano);
    }

    // =========================================================================
    // API: RELATÓRIO DETALHADO DO CONSELHEIRO (saldo, pontos, etc.)
    // =========================================================================
    @GetMapping("/relatorio-conselheiro/{idPessoa}/gestao/{idGestao}/mes/{mes}/ano/{ano}")
    @ResponseBody
    public Map<String, Object> relatorioConselheiro(
            @PathVariable Integer idPessoa,
            @PathVariable Integer idGestao,
            @PathVariable Integer mes,
            @PathVariable Integer ano) {

        Conselheiro conselheiro = conselheiroService.buscarPorId(idPessoa)
                .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado"));

        List<Map<String, Object>> atividades = jetonService
                .listarAtividadesAgrupadasPorConselheiro(idPessoa, idGestao, mes, ano);

        List<Jeton> jetons = jetonRepository.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano).stream()
                .filter(j -> j.getConselheiro().getIdPessoa().equals(idPessoa))
                .toList();

        int saldoAnterior = 0;
        int pontosAcumuladosMes = 0;
        for (Jeton j : jetons) {
            List<PontosSaldo> pontosList = pontosSaldoRepository.findByJetonIdJeton(j.getIdJeton());
            for (PontosSaldo ps : pontosList) {
                boolean doMesAtual = false;
                if (ps.getAtividade() != null) {
                    LocalDate dataAtv = ps.getAtividade().getDataHoraAtividade().toLocalDate();
                    if (dataAtv.getYear() == ano && dataAtv.getMonthValue() == mes) {
                        doMesAtual = true;
                    }
                }
                if (doMesAtual) {
                    pontosAcumuladosMes += ps.getPontosUtilizados();
                } else {
                    saldoAnterior += ps.getPontosUtilizados();
                }
            }
        }
        Integer saldoFuturo = pontosSaldoRepository.somarPontosSobrandoAtivos(idPessoa, idGestao);

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("nomeConselheiro", conselheiro.getPessoa().getNome());
        resposta.put("atividades", atividades);
        resposta.put("saldoAnterior", saldoAnterior);
        resposta.put("pontosAcumuladosMes", pontosAcumuladosMes);
        resposta.put("saldoFuturo", saldoFuturo != null ? saldoFuturo : 0);
        return resposta;
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================
    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }

    private Integer getIdUsuarioLogado(HttpSession session) {
        ViewUserLogin usuario = (ViewUserLogin) session.getAttribute("usuarioLogado");
        return usuario != null ? usuario.getIdPessoa() : null;
    }
}