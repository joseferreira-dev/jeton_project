package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.dto.AtividadeVinculadaDTO;
import br.com.cremepe.jeton.dto.JetonDTO;
import br.com.cremepe.jeton.dto.RelatorioConselheiroDTO;
import br.com.cremepe.jeton.dto.RelatorioGeralDTO;
import br.com.cremepe.jeton.mapper.JetonMapper;
import br.com.cremepe.jeton.service.JetonService;
import br.com.cremepe.jeton.service.GestaoService;
import br.com.cremepe.jeton.service.JetonRelatorioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jetons")
@PreAuthorize("isAuthenticated()")
public class JetonApiController {

    private final JetonService jetonService;
    private final JetonRelatorioService relatorioService;
    private final JetonMapper jetonMapper;
    private final GestaoService gestaoService;

    JetonApiController(JetonService jetonService, JetonRelatorioService relatorioService, JetonMapper jetonMapper,
            GestaoService gestaoService) {
        this.jetonService = jetonService;
        this.relatorioService = relatorioService;
        this.jetonMapper = jetonMapper;
        this.gestaoService = gestaoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('J') or hasAuthority('S')")
    public ResponseEntity<List<JetonDTO>> listarTodosJetons() {
        List<JetonDTO> jetons = jetonService.listarTodos();
        return ResponseEntity.ok(jetons);
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('J') or hasAuthority('S')")
    public ResponseEntity<Page<JetonDTO>> listarPaginado(
            @RequestParam(required = false) Integer idGestao,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) String termo,
            @PageableDefault(size = 10, sort = "ano,mes") Pageable pageable) {
        List<JetonDTO> lista = jetonService.pesquisarHistorico(idGestao, mes, ano, termo);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), lista.size());
        Page<JetonDTO> page = new org.springframework.data.domain.PageImpl<>(
                lista.subList(start, end), pageable, lista.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('J') or hasAuthority('S')")
    public ResponseEntity<JetonDTO> buscarPorId(@PathVariable Integer id) {
        JetonDTO dto = jetonService.buscarPorId(id)
                .map(j -> jetonMapper.toDto(j))
                .orElseThrow(() -> new RuntimeException("Jeton não encontrado"));
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/processar")
    @PreAuthorize("hasAuthority('J') or hasAuthority('S')")
    public ResponseEntity<Void> processarFolha(
            @RequestParam Integer idGestao,
            @RequestParam Integer mes,
            @RequestParam Integer ano) {
        Gestao gestao = gestaoService.buscarGestaoOuFalhar(idGestao);
        jetonService.processarFechamentoMensal(gestao, mes, ano);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fechar")
    @PreAuthorize("hasAuthority('J') or hasAuthority('S')")
    public ResponseEntity<Void> fecharFolha(
            @RequestParam Integer idGestao,
            @RequestParam Integer mes,
            @RequestParam Integer ano) {
        Gestao gestao = gestaoService.buscarGestaoOuFalhar(idGestao);
        jetonService.realizarFechamentoDefinitivoFolha(gestao, mes, ano);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/relatorio")
    @PreAuthorize("hasAuthority('J') or hasAuthority('S')")
    public ResponseEntity<RelatorioGeralDTO> obterRelatorioGeral(
            @RequestParam Integer idGestao,
            @RequestParam Integer mes,
            @RequestParam Integer ano) {
        RelatorioGeralDTO relatorio = jetonService.gerarRelatorioGeral(idGestao, mes, ano);
        return ResponseEntity.ok(relatorio);
    }

    @GetMapping("/relatorio/exportar")
    @PreAuthorize("hasAuthority('J') or hasAuthority('S')")
    public ResponseEntity<byte[]> exportarRelatorio(
            @RequestParam Integer idGestao,
            @RequestParam Integer mes,
            @RequestParam Integer ano,
            @RequestParam(defaultValue = "excel") String formato)
            throws IOException, com.lowagie.text.DocumentException {
        RelatorioGeralDTO dados = jetonService.gerarRelatorioGeral(idGestao, mes, ano);
        byte[] relatorio;
        String contentType;
        String extensao;

        if ("excel".equalsIgnoreCase(formato)) {
            relatorio = relatorioService.gerarExcelRelatorio(dados);
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            extensao = "xlsx";
        } else {
            relatorio = relatorioService.gerarPdfRelatorio(dados);
            contentType = MediaType.APPLICATION_PDF_VALUE;
            extensao = "pdf";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"relatorio_jetons_" + mes + "_" + ano + "." + extensao + "\"")
                .body(relatorio);
    }

    @GetMapping("/extrato/conselheiro/{idConselheiro}")
    @PreAuthorize("hasAuthority('J') or hasAuthority('S') or #idConselheiro == authentication.principal.viewUserLogin.idPessoa")
    public ResponseEntity<Map<String, Object>> extratoPontosConselheiro(
            @PathVariable Integer idConselheiro,
            @RequestParam Integer idGestao,
            @RequestParam Integer mes,
            @RequestParam Integer ano) {
        var relatorio = jetonService.gerarRelatorioIndividualConselheiro(idConselheiro, idGestao, mes, ano);
        return ResponseEntity.ok(Map.of(
                "conselheiro", relatorio.nomeConselheiro(),
                "atividades", relatorio.atividades(),
                "saldoExistente", relatorio.saldoExistente(),
                "saldoAtividades", relatorio.saldoAtividades(),
                "saldoUtilizado", relatorio.saldoUtilizado(),
                "saldoFuturo", relatorio.saldoFuturo()));
    }

    @GetMapping("/atividades/conselheiro/{idPessoa}/gestao/{idGestao}/mes/{mes}/ano/{ano}")
    public List<AtividadeVinculadaDTO> obterAtividadesVinculadas(
            @PathVariable("idPessoa") Integer idPessoa,
            @PathVariable("idGestao") Integer idGestao,
            @PathVariable("mes") Integer mes,
            @PathVariable("ano") Integer ano) {
        return jetonService.listarAtividadesAgrupadasPorConselheiro(idPessoa, idGestao, mes, ano);
    }

    @GetMapping("/relatorio-conselheiro/{idPessoa}/gestao/{idGestao}/mes/{mes}/ano/{ano}")
    public RelatorioConselheiroDTO relatorioConselheiro(
            @PathVariable Integer idPessoa,
            @PathVariable Integer idGestao,
            @PathVariable Integer mes,
            @PathVariable Integer ano) {
        return jetonService.gerarRelatorioIndividualConselheiro(idPessoa, idGestao, mes, ano);
    }
}