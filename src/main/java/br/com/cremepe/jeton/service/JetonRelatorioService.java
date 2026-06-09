package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.dto.AtividadeRelatorioDTO;
import br.com.cremepe.jeton.dto.ConselheiroRelatorioDTO;
import br.com.cremepe.jeton.dto.RelatorioGeralDTO;
import br.com.cremepe.jeton.util.ExcelStyleHelper;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class JetonRelatorioService {

    private static final int MAX_COLUMN_WIDTH = 20000;

    public byte[] gerarExcelRelatorio(RelatorioGeralDTO dados) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Estilos reutilizáveis
            XSSFCellStyle headerStyle = ExcelStyleHelper.criarEstiloCabecalho(workbook);
            XSSFCellStyle moneyStyle = ExcelStyleHelper.criarEstiloDinheiro(workbook);
            XSSFCellStyle numberStyle = ExcelStyleHelper.criarEstiloNumero(workbook);
            XSSFCellStyle leftStyle = ExcelStyleHelper.criarEstiloTextoEsquerda(workbook);
            XSSFCellStyle centerStyle = ExcelStyleHelper.criarEstiloTextoCentro(workbook);
            XSSFCellStyle totalStyle = ExcelStyleHelper.criarEstiloTotal(workbook);
            XSSFCellStyle totalMoneyStyle = ExcelStyleHelper.criarEstiloTotalDinheiro(workbook);

            // ==================== ABA RESUMO ====================
            XSSFSheet sheetResumo = workbook.createSheet("Resumo Geral");
            int rowNum = 0;

            // Título
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

            // Mescla células vazias
            for (int i = 3; i <= 5; i++) {
                XSSFCell emptyCell = totalRow.createCell(i);
                emptyCell.setCellStyle(totalStyle);
            }

            // Ajuste de largura das colunas
            for (int i = 0; i < 6; i++) {
                sheetResumo.autoSizeColumn(i);
                int width = sheetResumo.getColumnWidth(i);
                sheetResumo.setColumnWidth(i, Math.min(width + 1024, MAX_COLUMN_WIDTH));
            }

            // ==================== ABAS INDIVIDUAIS ====================
            for (ConselheiroRelatorioDTO c : dados.getConselheiros()) {
                String sheetName = c.getNome();
                if (sheetName.length() > 31) {
                    sheetName = sheetName.substring(0, 31);
                }
                XSSFSheet sheetDet = workbook.createSheet(sheetName);
                int detRow = 0;

                // Título
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

                // Ajuste de largura
                for (int i = 0; i < 3; i++) {
                    sheetDet.autoSizeColumn(i);
                    int width = sheetDet.getColumnWidth(i);
                    sheetDet.setColumnWidth(i, Math.min(width + 1024, MAX_COLUMN_WIDTH));
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] gerarPdfRelatorio(RelatorioGeralDTO dados) throws DocumentException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Cabeçalho e rodapé
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

            // Título
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Paragraph title = new Paragraph("RELATÓRIO DE PROCESSAMENTO DE JETONS", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Informações
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Paragraph info = new Paragraph(
                    String.format("Gestão: %s | Competência: %d/%d | Gerado em: %s",
                            dados.getNomeGestao(), dados.getMes(), dados.getAno(),
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))),
                    infoFont);
            info.setAlignment(Element.ALIGN_CENTER);
            info.setSpacingAfter(20);
            document.add(info);

            // Tabela resumo
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

            // Páginas individuais por conselheiro
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

    private PdfPCell createCell(String text, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }
}