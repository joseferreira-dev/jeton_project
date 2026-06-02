package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dominio.Jeton;
import br.com.cremepe.jeton.dominio.PontosSaldo;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.dto.AtividadeRelatorioDTO;
import br.com.cremepe.jeton.dto.ConselheiroRelatorioDTO;
import br.com.cremepe.jeton.dto.RelatorioGeralDTO;
import br.com.cremepe.jeton.repositorio.JetonRepository;
import br.com.cremepe.jeton.repositorio.PontosSaldoRepository;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.JetonService;
import jakarta.servlet.http.HttpSession;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @GetMapping("/relatorio")
    public ResponseEntity<byte[]> gerarRelatorio(
            @RequestParam Integer idGestao,
            @RequestParam Integer mes,
            @RequestParam Integer ano,
            @RequestParam(defaultValue = "excel") String formato) throws IOException, DocumentException {
        RelatorioGeralDTO dados = jetonService.gerarRelatorioGeral(idGestao, mes, ano);

        if ("excel".equalsIgnoreCase(formato)) {
            byte[] excel = gerarExcelRelatorio(dados);
            return ResponseEntity.ok()
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"relatorio_jetons_" + mes + "_" + ano + ".xlsx\"")
                    .body(excel);
        } else if ("pdf".equalsIgnoreCase(formato)) {
            byte[] pdf = gerarPdfRelatorio(dados);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"relatorio_jetons_" + mes + "_" + ano + ".pdf\"")
                    .body(pdf);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    private byte[] gerarExcelRelatorio(RelatorioGeralDTO dados) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Criação de estilos
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setAlignment(HorizontalAlignment.RIGHT);
            moneyStyle.setBorderBottom(BorderStyle.THIN);
            moneyStyle.setBorderTop(BorderStyle.THIN);
            moneyStyle.setBorderLeft(BorderStyle.THIN);
            moneyStyle.setBorderRight(BorderStyle.THIN);
            XSSFDataFormat format = workbook.createDataFormat();
            moneyStyle.setDataFormat(format.getFormat("R$ #,##0.00"));

            XSSFCellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);
            numberStyle.setBorderBottom(BorderStyle.THIN);
            numberStyle.setBorderTop(BorderStyle.THIN);
            numberStyle.setBorderLeft(BorderStyle.THIN);
            numberStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle leftStyle = workbook.createCellStyle();
            leftStyle.setAlignment(HorizontalAlignment.LEFT);
            leftStyle.setBorderBottom(BorderStyle.THIN);
            leftStyle.setBorderTop(BorderStyle.THIN);
            leftStyle.setBorderLeft(BorderStyle.THIN);
            leftStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setBorderBottom(BorderStyle.THIN);
            centerStyle.setBorderTop(BorderStyle.THIN);
            centerStyle.setBorderLeft(BorderStyle.THIN);
            centerStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle totalStyle = workbook.createCellStyle();
            totalStyle.setAlignment(HorizontalAlignment.CENTER);
            totalStyle.setFont(headerFont);
            totalStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalStyle.setBorderBottom(BorderStyle.THIN);
            totalStyle.setBorderTop(BorderStyle.THIN);
            totalStyle.setBorderLeft(BorderStyle.THIN);
            totalStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle totalMoneyStyle = workbook.createCellStyle();
            totalMoneyStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalMoneyStyle.setFont(headerFont);
            totalMoneyStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            totalMoneyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalMoneyStyle.setBorderBottom(BorderStyle.THIN);
            totalMoneyStyle.setBorderTop(BorderStyle.THIN);
            totalMoneyStyle.setBorderLeft(BorderStyle.THIN);
            totalMoneyStyle.setBorderRight(BorderStyle.THIN);
            totalMoneyStyle.setDataFormat(format.getFormat("R$ #,##0.00"));

            // ==================== ABA RESUMO ====================
            XSSFSheet sheetResumo = workbook.createSheet("Resumo Geral");
            int rowNum = 0;

            // Título da aba
            XSSFRow titleRow = sheetResumo.createRow(rowNum++);
            XSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("RELATÓRIO DE PROCESSAMENTO DE JETONS");
            XSSFCellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheetResumo.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            rowNum++; // linha em branco

            // Informações da gestão e competência
            XSSFRow infoRow = sheetResumo.createRow(rowNum++);
            XSSFCell infoCell = infoRow.createCell(0);
            infoCell.setCellValue(String.format("Gestão: %s | Competência: %d/%d | Gerado em: %s",
                    dados.getNomeGestao(), dados.getMes(), dados.getAno(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
            infoCell.setCellStyle(leftStyle);
            sheetResumo.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

            rowNum++; // linha em branco

            // Cabeçalho da tabela
            XSSFRow header = sheetResumo.createRow(rowNum++);
            String[] headers = { "Conselheiro", "Total Jetons", "Valor Total (R$)", "Pontos Anteriores",
                    "Pontos do Mês", "Saldo Futuro" };
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dados
            for (ConselheiroRelatorioDTO c : dados.getConselheiros()) {
                XSSFRow row = sheetResumo.createRow(rowNum++);

                XSSFCell nomeCell = row.createCell(0);
                nomeCell.setCellValue(c.getNome());
                nomeCell.setCellStyle(leftStyle);

                XSSFCell jetonsCell = row.createCell(1);
                jetonsCell.setCellValue(c.getTotalJetons());
                jetonsCell.setCellStyle(centerStyle);

                XSSFCell valorCell = row.createCell(2);
                valorCell.setCellValue(c.getValor().doubleValue());
                valorCell.setCellStyle(moneyStyle);

                XSSFCell anteriorCell = row.createCell(3);
                anteriorCell.setCellValue(c.getSaldoAnterior());
                anteriorCell.setCellStyle(numberStyle);

                XSSFCell mesCell = row.createCell(4);
                mesCell.setCellValue(c.getPontosAcumuladosMes());
                mesCell.setCellStyle(numberStyle);

                XSSFCell futuroCell = row.createCell(5);
                futuroCell.setCellValue(c.getSaldoFuturo());
                futuroCell.setCellStyle(numberStyle);
            }

            // Linha de totais
            XSSFRow totalRow = sheetResumo.createRow(rowNum++);
            XSSFCell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TOTAIS GERAIS");
            totalLabel.setCellStyle(totalStyle);

            XSSFCell totalJetonsCell = totalRow.createCell(1);
            totalJetonsCell.setCellValue(dados.getTotalGeralJetons());
            totalJetonsCell.setCellStyle(totalStyle);

            XSSFCell totalValorCell = totalRow.createCell(2);
            totalValorCell.setCellValue(dados.getTotalGeralValor().doubleValue());
            totalValorCell.setCellStyle(totalMoneyStyle);

            // Mescla células vazias para manter o alinhamento visual
            for (int i = 3; i <= 5; i++) {
                XSSFCell emptyCell = totalRow.createCell(i);
                emptyCell.setCellStyle(totalStyle);
            }

            // Auto-ajuste das colunas
            for (int i = 0; i < 6; i++) {
                sheetResumo.autoSizeColumn(i);
                // Aumenta um pouco a largura para não cortar texto
                int width = sheetResumo.getColumnWidth(i);
                sheetResumo.setColumnWidth(i, Math.min(width + 1024, 20000));
            }

            // ==================== ABAS INDIVIDUAIS ====================
            for (ConselheiroRelatorioDTO c : dados.getConselheiros()) {
                String sheetName = c.getNome();
                // Limita nome da aba a 31 caracteres (regra do Excel)
                if (sheetName.length() > 31) {
                    sheetName = sheetName.substring(0, 31);
                }
                XSSFSheet sheetDet = workbook.createSheet(sheetName);
                int detRow = 0;

                // Título do conselheiro
                XSSFRow titleDet = sheetDet.createRow(detRow++);
                XSSFCell titleDetCell = titleDet.createCell(0);
                titleDetCell.setCellValue("Conselheiro: " + c.getNome());
                titleDetCell.setCellStyle(titleStyle);
                sheetDet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

                detRow++; // linha em branco

                // Resumo de pontos
                XSSFRow pontosRow = sheetDet.createRow(detRow++);
                XSSFCell pontosCell = pontosRow.createCell(0);
                pontosCell.setCellValue(
                        String.format("Saldo anterior utilizado: %d pts | Pontos do mês: %d pts | Saldo futuro: %d pts",
                                c.getSaldoAnterior(), c.getPontosAcumuladosMes(), c.getSaldoFuturo()));
                pontosCell.setCellStyle(leftStyle);
                sheetDet.addMergedRegion(new CellRangeAddress(detRow - 1, detRow - 1, 0, 2));

                detRow++; // linha em branco

                // Cabeçalho da tabela de atividades
                XSSFRow headerDet = sheetDet.createRow(detRow++);
                XSSFCell regraHeader = headerDet.createCell(0);
                regraHeader.setCellValue("Atividade");
                regraHeader.setCellStyle(headerStyle);
                XSSFCell dataHeader = headerDet.createCell(1);
                dataHeader.setCellValue("Data");
                dataHeader.setCellStyle(headerStyle);
                XSSFCell qtdHeader = headerDet.createCell(2);
                qtdHeader.setCellValue("Quantidade");
                qtdHeader.setCellStyle(headerStyle);

                // Dados das atividades
                if (c.getAtividades().isEmpty()) {
                    XSSFRow emptyRow = sheetDet.createRow(detRow++);
                    XSSFCell emptyCell = emptyRow.createCell(0);
                    emptyCell.setCellValue("Nenhuma atividade registrada no mês");
                    emptyCell.setCellStyle(centerStyle);
                    sheetDet.addMergedRegion(new CellRangeAddress(detRow - 1, detRow - 1, 0, 2));
                } else {
                    for (AtividadeRelatorioDTO a : c.getAtividades()) {
                        XSSFRow row = sheetDet.createRow(detRow++);
                        XSSFCell regraCell = row.createCell(0);
                        regraCell.setCellValue(a.getRegra());
                        regraCell.setCellStyle(leftStyle);
                        XSSFCell dataCell = row.createCell(1);
                        dataCell.setCellValue(a.getData().toString());
                        dataCell.setCellStyle(centerStyle);
                        XSSFCell qtdCell = row.createCell(2);
                        qtdCell.setCellValue(a.getQuantidade());
                        qtdCell.setCellStyle(centerStyle);
                    }
                }

                // Auto-ajuste das colunas
                for (int i = 0; i < 3; i++) {
                    sheetDet.autoSizeColumn(i);
                    int width = sheetDet.getColumnWidth(i);
                    sheetDet.setColumnWidth(i, Math.min(width + 1024, 20000));
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] gerarPdfRelatorio(RelatorioGeralDTO dados) throws DocumentException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
                    PdfPTable header = new PdfPTable(1);
                    header.setTotalWidth(document.getPageSize().getWidth() - 72);
                    header.setLockedWidth(true);
                    PdfPCell cell = new PdfPCell(
                            new Phrase("CREMEPE - Sistema Jeton - Relatório Detalhado", headerFont));
                    cell.setBackgroundColor(new Color(0, 100, 0));
                    cell.setBorder(Rectangle.NO_BORDER);
                    cell.setPadding(5);
                    header.addCell(cell);
                    header.writeSelectedRows(0, -1, 36, document.getPageSize().getHeight() - 20,
                            writer.getDirectContent());

                    Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
                    PdfPTable footer = new PdfPTable(1);
                    footer.setTotalWidth(document.getPageSize().getWidth() - 72);
                    footer.setLockedWidth(true);
                    PdfPCell footerCell = new PdfPCell(new Phrase("Página " + writer.getPageNumber(), footerFont));
                    footerCell.setBorder(Rectangle.NO_BORDER);
                    footerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    footer.addCell(footerCell);
                    footer.writeSelectedRows(0, -1, 36, 20, writer.getDirectContent());
                }
            });

            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Paragraph title = new Paragraph("RELATÓRIO DE PROCESSAMENTO DE JETONS", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Paragraph info = new Paragraph(
                    String.format("Gestão: %s | Competência: %d/%d | Gerado em: %s",
                            dados.getNomeGestao(),
                            dados.getMes(),
                            dados.getAno(),
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))),
                    infoFont);
            info.setAlignment(Element.ALIGN_CENTER);
            info.setSpacingAfter(20);
            document.add(info);

            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            PdfPTable summaryTable = new PdfPTable(6);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[] { 30, 10, 15, 15, 15, 15 });

            String[] headers = { "Conselheiro", "Total Jetons", "Valor (R$)", "Pontos Anteriores", "Pontos do Mês",
                    "Saldo Futuro" };
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, tableHeaderFont));
                cell.setBackgroundColor(new Color(0, 100, 0));
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                summaryTable.addCell(cell);
            }

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (ConselheiroRelatorioDTO c : dados.getConselheiros()) {
                summaryTable.addCell(createCell(c.getNome(), Element.ALIGN_LEFT, cellFont));
                summaryTable.addCell(createCell(String.valueOf(c.getTotalJetons()), Element.ALIGN_CENTER, cellFont));
                summaryTable.addCell(
                        createCell(String.format("%.2f", c.getValor().doubleValue()), Element.ALIGN_RIGHT, cellFont));
                summaryTable.addCell(createCell(String.valueOf(c.getSaldoAnterior()), Element.ALIGN_RIGHT, cellFont));
                summaryTable
                        .addCell(createCell(String.valueOf(c.getPontosAcumuladosMes()), Element.ALIGN_RIGHT, cellFont));
                summaryTable.addCell(createCell(String.valueOf(c.getSaldoFuturo()), Element.ALIGN_RIGHT, cellFont));
            }

            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            PdfPCell totalCell = new PdfPCell(new Phrase("TOTAIS GERAIS", totalFont));
            totalCell.setColspan(2);
            totalCell.setBackgroundColor(new Color(200, 200, 200));
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.addCell(totalCell);
            summaryTable.addCell(createCell(String.format("%.2f", dados.getTotalGeralValor().doubleValue()),
                    Element.ALIGN_RIGHT, cellFont));
            summaryTable.addCell(createCell("", Element.ALIGN_RIGHT, cellFont));
            summaryTable.addCell(createCell("", Element.ALIGN_RIGHT, cellFont));
            summaryTable.addCell(createCell("", Element.ALIGN_RIGHT, cellFont));

            document.add(summaryTable);
            document.add(new Paragraph(" "));

            for (ConselheiroRelatorioDTO c : dados.getConselheiros()) {
                document.newPage();

                Paragraph subTitle = new Paragraph("Conselheiro: " + c.getNome(), titleFont);
                subTitle.setAlignment(Element.ALIGN_LEFT);
                subTitle.setSpacingAfter(10);
                document.add(subTitle);

                Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
                Paragraph pontosResumo = new Paragraph(
                        String.format("Saldo anterior utilizado: %d pts | Pontos do mês: %d pts | Saldo futuro: %d pts",
                                c.getSaldoAnterior(), c.getPontosAcumuladosMes(), c.getSaldoFuturo()),
                        summaryFont);
                pontosResumo.setSpacingAfter(15);
                document.add(pontosResumo);

                PdfPTable activityTable = new PdfPTable(3);
                activityTable.setWidthPercentage(100);
                activityTable.setWidths(new float[] { 60, 20, 20 });

                PdfPCell actHeader1 = new PdfPCell(new Phrase("Atividade", tableHeaderFont));
                actHeader1.setBackgroundColor(new Color(0, 100, 0));
                actHeader1.setHorizontalAlignment(Element.ALIGN_CENTER);
                activityTable.addCell(actHeader1);

                PdfPCell actHeader2 = new PdfPCell(new Phrase("Data", tableHeaderFont));
                actHeader2.setBackgroundColor(new Color(0, 100, 0));
                actHeader2.setHorizontalAlignment(Element.ALIGN_CENTER);
                activityTable.addCell(actHeader2);

                PdfPCell actHeader3 = new PdfPCell(new Phrase("Quantidade", tableHeaderFont));
                actHeader3.setBackgroundColor(new Color(0, 100, 0));
                actHeader3.setHorizontalAlignment(Element.ALIGN_CENTER);
                activityTable.addCell(actHeader3);

                Font actFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
                if (c.getAtividades().isEmpty()) {
                    PdfPCell emptyCell = new PdfPCell(new Phrase("Nenhuma atividade registrada no mês", actFont));
                    emptyCell.setColspan(3);
                    emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    activityTable.addCell(emptyCell);
                } else {
                    for (AtividadeRelatorioDTO a : c.getAtividades()) {
                        activityTable.addCell(createCell(a.getRegra(), Element.ALIGN_LEFT, actFont));
                        activityTable.addCell(createCell(a.getData().toString(), Element.ALIGN_CENTER, actFont));
                        activityTable
                                .addCell(createCell(String.valueOf(a.getQuantidade()), Element.ALIGN_CENTER, actFont));
                    }
                }
                document.add(activityTable);
            }

            document.close();
            return out.toByteArray();
        }
    }

    // Método auxiliar para criar células com estilo
    private PdfPCell createCell(String text, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        return cell;
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