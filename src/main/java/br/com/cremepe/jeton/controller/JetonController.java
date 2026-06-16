package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.dto.JetonAgrupadoDTO;
import br.com.cremepe.jeton.dto.JetonDTO;
import br.com.cremepe.jeton.dto.RelatorioGeralDTO;
import br.com.cremepe.jeton.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lowagie.text.DocumentException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/jeton")
@PreAuthorize("hasAuthority('J') or hasAuthority('S')")
public class JetonController {

    private final JetonService jetonService;
    private final GestaoService gestaoService;
    private final JetonRelatorioService relatorioService;

    JetonController(JetonService jetonService, GestaoService gestaoService, JetonRelatorioService relatorioService) {
        this.jetonService = jetonService;
        this.gestaoService = gestaoService;
        this.relatorioService = relatorioService;
    }

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

        List<JetonAgrupadoDTO> jetonsAgrupados = List.of();
        if (idGestao != null) {
            jetonsAgrupados = jetonService.listarJetonsAgrupadosPorGestaoEMes(idGestao, mesFiltro, anoFiltro);
            model.addAttribute("idGestaoSelecionada", idGestao);
        }

        model.addAttribute("listaJetons", jetonsAgrupados);
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("mesAtual", mesFiltro);
        model.addAttribute("anoAtual", anoFiltro);
        return "jeton/lista";
    }

    @PostMapping("/processar")
    public String processar(@RequestParam("idGestao") Integer idGestao,
            @RequestParam("mes") Integer mes,
            @RequestParam("ano") Integer ano,
            HttpSession session,
            RedirectAttributes ra) {
        if (naoAutenticado(session))
            return "redirect:/login";

        Optional<Gestao> gestaoOpt = gestaoService.buscarPorId(idGestao);
        if (gestaoOpt.isEmpty())
            throw new RuntimeException("Gestão não encontrada.");
        jetonService.processarFechamentoMensal(gestaoOpt.get(), mes, ano);
        ra.addFlashAttribute("sucesso", "Cálculo da folha mensal executado com sucesso!");
        return "redirect:/jeton?idGestao=" + idGestao + "&mes=" + mes + "&ano=" + ano;
    }

    @PostMapping("/fechar-definitivo")
    public String fecharDefinitivo(@RequestParam("idGestao") Integer idGestao,
            @RequestParam("mes") Integer mes,
            @RequestParam("ano") Integer ano,
            HttpSession session,
            RedirectAttributes ra) {
        if (naoAutenticado(session))
            return "redirect:/login";

        Optional<Gestao> gestaoOpt = gestaoService.buscarPorId(idGestao);
        if (gestaoOpt.isEmpty())
            throw new RuntimeException("Gestão não encontrada.");
        jetonService.realizarFechamentoDefinitivoFolha(gestaoOpt.get(), mes, ano);
        ra.addFlashAttribute("sucesso", "Folha fechada e homologada.");
        return "redirect:/jeton";
    }

    @GetMapping("/estornar/{id}")
    public String estornarJeton(@PathVariable("id") Integer idJeton, HttpSession session, RedirectAttributes ra) {
        if (naoAutenticado(session))
            return "redirect:/login";

        jetonService.estornarJetonPontual(idJeton);
        ra.addFlashAttribute("sucesso", "Jeton estornado com sucesso!");
        return "redirect:/jeton";
    }

    @GetMapping("/historico")
    public String exibirHistorico(
            @RequestParam(value = "idGestao", required = false) Integer idGestao,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano,
            @RequestParam(value = "termo", required = false) String termo,
            Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";

        List<JetonDTO> historico = jetonService.pesquisarHistorico(idGestao, mes, ano, termo);
        model.addAttribute("listaJetons", historico);
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("idGestaoSelecionada", idGestao);
        model.addAttribute("mesSelecionado", mes);
        model.addAttribute("anoSelecionado", ano);
        model.addAttribute("termo", termo);
        return "jeton/historico";
    }

    @GetMapping("/relatorio")
    public ResponseEntity<byte[]> gerarRelatorio(
            @RequestParam Integer idGestao,
            @RequestParam Integer mes,
            @RequestParam Integer ano,
            @RequestParam(defaultValue = "excel") String formato) throws IOException, DocumentException {

        RelatorioGeralDTO dados = jetonService.gerarRelatorioGeral(idGestao, mes, ano);
        byte[] relatorio;

        if ("excel".equalsIgnoreCase(formato)) {
            relatorio = relatorioService.gerarExcelRelatorio(dados);
            return ResponseEntity.ok()
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"relatorio_jetons_" + mes + "_" + ano + ".xlsx\"")
                    .body(relatorio);
        } else if ("pdf".equalsIgnoreCase(formato)) {
            relatorio = relatorioService.gerarPdfRelatorio(dados);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"relatorio_jetons_" + mes + "_" + ano + ".pdf\"")
                    .body(relatorio);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }
}