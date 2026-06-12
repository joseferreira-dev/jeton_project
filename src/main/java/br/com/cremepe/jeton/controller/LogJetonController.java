package br.com.cremepe.jeton.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.cremepe.jeton.service.LogJetonService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.TreeMap;

@Controller
@RequestMapping("/logs")
@PreAuthorize("hasAuthority('N') or hasAuthority('S')")
public class LogJetonController {

    private final LogJetonService logJetonService;

    LogJetonController(LogJetonService logJetonService) {
        this.logJetonService = logJetonService;
    }

    @GetMapping
    public String listar(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "nomeTabela", required = false) String nomeTabela,
            @RequestParam(value = "dataInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(value = "dataFim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Model model, HttpSession session) {

        if (session.getAttribute("usuarioLogado") == null) {
            return "redirect:/login";
        }

        if (nomeTabela != null && nomeTabela.trim().isEmpty()) {
            nomeTabela = null;
        }

        LocalDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = (dataFim != null) ? dataFim.atTime(LocalTime.MAX) : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dataHoraLog"));
        Page<?> paginaLogs = logJetonService.listarComFiltros(nomeTabela, inicio, fim, pageable);

        Map<String, String> opcoesTabelas = new TreeMap<>();
        opcoesTabelas.put("acesso_negado", "Acessos Negados");
        opcoesTabelas.put("atividade_conselhal", "Atividade Conselhal");
        opcoesTabelas.put("comprovante", "Comprovante");
        opcoesTabelas.put("conselheiro", "Conselheiro");
        opcoesTabelas.put("file_storage", "Arquivos FTP");
        opcoesTabelas.put("gestao", "Gestão");
        opcoesTabelas.put("gestao_conselheiro", "Vínculo Gestão/Conselheiro");
        opcoesTabelas.put("jeton", "Jeton (Financeiro)");
        opcoesTabelas.put("login", "Login/Logout");
        opcoesTabelas.put("nivel_acesso", "Nível de Acesso");
        opcoesTabelas.put("pontos_saldo", "Pontos Saldo");
        opcoesTabelas.put("portaria", "Portaria");
        opcoesTabelas.put("regras", "Regras");
        opcoesTabelas.put("regras_conjuntas", "Regras Conjuntas");
        opcoesTabelas.put("resolucao", "Resolução");
        opcoesTabelas.put("tipo_anexo", "Tipo de Anexo");
        opcoesTabelas.put("usuario", "Usuário");
        opcoesTabelas.put("usuario_acesso", "Permissões");

        model.addAttribute("paginaLogs", paginaLogs);
        model.addAttribute("nomeTabela", nomeTabela);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("size", size);
        model.addAttribute("opcoesTabelas", opcoesTabelas);

        return "log/lista";
    }
}