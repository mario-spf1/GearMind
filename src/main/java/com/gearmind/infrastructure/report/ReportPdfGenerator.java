package com.gearmind.infrastructure.report;

import com.gearmind.presentation.report.ReportItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReportPdfGenerator {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.getDefault()));

    public Path generate(String title, List<ReportItem> items, String filtersSummary) {
        try {
            Path baseDir = ReportPdfStorage.baseDir();
            Files.createDirectories(baseDir);

            Path outputPath = ReportPdfStorage.resolvePath(title);

            Document document = new Document(PageSize.A4.rotate(), 36, 36, 48, 36);
            PdfWriter.getInstance(document, new FileOutputStream(outputPath.toFile()));
            document.open();

            document.add(new Paragraph(title == null ? "Reporte" : title, TITLE_FONT));
            document.add(new Paragraph("Generado: " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()), SUBTITLE_FONT));
            if (filtersSummary != null && !filtersSummary.isBlank()) {
                document.add(new Paragraph(filtersSummary, SUBTITLE_FONT));
            }
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(new float[]{1.2f, 1.6f, 3.4f, 1.4f, 1.4f, 1.2f, 1.3f});
            table.setWidthPercentage(100);
            addHeaderCell(table, "Empresa");
            addHeaderCell(table, "Tipo");
            addHeaderCell(table, "Descripción");
            addHeaderCell(table, "Fecha");
            addHeaderCell(table, "Estado");
            addHeaderCell(table, "Cantidad/Stock");
            addHeaderCell(table, "Total");

            for (ReportItem item : items) {
                table.addCell(cell(item.getEmpresa()));
                table.addCell(cell(item.getTipo()));
                table.addCell(cell(item.getDescripcion()));
                table.addCell(cell(item.getFechaLabel()));
                table.addCell(cell(item.getEstado()));
                table.addCell(cell(item.getCantidadLabel()));
                table.addCell(cell(item.getTotal() == null ? "" : MONEY_FORMAT.format(item.getTotal())));
            }

            document.add(table);
            document.close();
            return outputPath;
        } catch (Exception e) {
            throw new RuntimeException("Error generando el PDF del reporte", e);
        }
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private PdfPCell cell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, CELL_FONT));
        cell.setPadding(5);
        return cell;
    }
}
