package com.gearmind.infrastructure.budget;

import com.gearmind.domain.budget.Budget;
import com.gearmind.domain.budget.BudgetLine;
import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.vehicle.Vehicle;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class BudgetPdfGenerator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(new Locale("es", "ES")));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public Path generate(Budget budget, List<BudgetLine> lines, Empresa empresa, Customer customer, Vehicle vehicle) {
        try {
            Path baseDir = BudgetPdfStorage.baseDir();
            Files.createDirectories(baseDir);
            Path outputPath = BudgetPdfStorage.resolvePath(budget.getId());
            Document document = new Document(PageSize.A4, 36, 36, 48, 36);
            PdfWriter.getInstance(document, new FileOutputStream(outputPath.toFile()));
            document.open();
            addHeader(document, budget, empresa);
            addCustomerSection(document, customer, vehicle);
            addLinesTable(document, lines);
            addTotals(document, budget.getTotalEstimado());
            addObservations(document, budget.getObservaciones());
            document.close();
            return outputPath;
        } catch (Exception e) {
            throw new RuntimeException("Error generando el PDF del presupuesto", e);
        }
    }

    private void addHeader(Document document, Budget budget, Empresa empresa) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{60, 40});
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        Paragraph companyName = new Paragraph(empresa != null ? empresa.getNombre() : "Empresa", titleFont);
        left.addElement(companyName);
        if (empresa != null) {
            left.addElement(new Paragraph("CIF: " + nullSafe(empresa.getCif()), subtitleFont));
            left.addElement(new Paragraph(nullSafe(empresa.getDireccion()), subtitleFont));
            left.addElement(new Paragraph(formatLocation(empresa), subtitleFont));
            left.addElement(new Paragraph("Tel: " + nullSafe(empresa.getTelefono()) + "  |  " + nullSafe(empresa.getEmail()), subtitleFont));
        }
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph docTitle = new Paragraph("Presupuesto", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
        docTitle.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(docTitle);
        right.addElement(new Paragraph("Nº " + budget.getId(), subtitleFont));
        if (budget.getFecha() != null) {
            right.addElement(new Paragraph("Fecha: " + budget.getFecha().format(DATE_FORMAT), subtitleFont));
        }
        right.addElement(new Paragraph("Estado: " + formatStatus(budget), subtitleFont));
        header.addCell(left);
        header.addCell(right);
        header.setSpacingAfter(18);
        document.add(header);
    }

    private void addCustomerSection(Document document, Customer customer, Vehicle vehicle) throws DocumentException {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setWidths(new float[]{50, 50});
        PdfPCell clienteCell = new PdfPCell();
        clienteCell.setBorder(Rectangle.NO_BORDER);
        clienteCell.addElement(new Paragraph("Cliente", labelFont));
        if (customer != null) {
            clienteCell.addElement(new Paragraph(customer.getNombre(), textFont));
            if (customer.getEmail() != null) {
                clienteCell.addElement(new Paragraph(customer.getEmail(), textFont));
            }
            if (customer.getTelefono() != null) {
                clienteCell.addElement(new Paragraph("Tel: " + customer.getTelefono(), textFont));
            }
        }

        PdfPCell vehiculoCell = new PdfPCell();
        vehiculoCell.setBorder(Rectangle.NO_BORDER);
        vehiculoCell.addElement(new Paragraph("Vehículo", labelFont));
        if (vehicle != null) {
            vehiculoCell.addElement(new Paragraph(vehicleLabel(vehicle), textFont));
            if (vehicle.getVin() != null && !vehicle.getVin().isBlank()) {
                vehiculoCell.addElement(new Paragraph("VIN: " + vehicle.getVin(), textFont));
            }
        }

        info.addCell(clienteCell);
        info.addCell(vehiculoCell);
        info.setSpacingAfter(16);
        document.add(info);
    }

    private void addLinesTable(Document document, List<BudgetLine> lines) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 15, 17, 18});
        addHeaderCell(table, "Descripción");
        addHeaderCell(table, "Cantidad");
        addHeaderCell(table, "Precio");
        addHeaderCell(table, "Total");
        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        for (BudgetLine line : lines) {
            table.addCell(new PdfPCell(new Phrase(line.getDescripcion(), rowFont)));
            table.addCell(cellRight(formatDecimal(line.getCantidad()), rowFont));
            table.addCell(cellRight(formatMoney(line.getPrecio()), rowFont));
            table.addCell(cellRight(formatMoney(line.getTotal()), rowFont));
        }
        table.setSpacingAfter(10);
        document.add(table);
    }

    private void addTotals(Document document, BigDecimal total) throws DocumentException {
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(40);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setWidths(new float[]{60, 40});
        PdfPCell label = new PdfPCell(new Phrase("Total estimado", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        label.setBorder(Rectangle.NO_BORDER);
        label.setHorizontalAlignment(Element.ALIGN_RIGHT);
        PdfPCell value = new PdfPCell(new Phrase(formatMoney(total), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        value.setBorder(Rectangle.NO_BORDER);
        value.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.addCell(label);
        totals.addCell(value);
        totals.setSpacingAfter(12);
        document.add(totals);
    }

    private void addObservations(Document document, String observaciones) throws DocumentException {
        if (observaciones == null || observaciones.isBlank()) {
            return;
        }
        Paragraph obsTitle = new Paragraph("Observaciones", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
        Paragraph obsText = new Paragraph(observaciones, FontFactory.getFont(FontFactory.HELVETICA, 10));
        document.add(obsTitle);
        document.add(obsText);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(60, 64, 67));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private PdfPCell cellRight(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0,00";
        }
        return MONEY_FORMAT.format(value);
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatStatus(Budget budget) {
        if (budget.getEstado() == null) {
            return "Borrador";
        }
        return switch (budget.getEstado()) {
            case BORRADOR ->
                "Borrador";
            case ENVIADO ->
                "Enviado";
            case ACEPTADO ->
                "Aceptado";
            case RECHAZADO ->
                "Rechazado";
        };
    }

    private String vehicleLabel(Vehicle vehicle) {
        if (vehicle == null) {
            return "";
        }
        String label = String.format("%s %s", nullSafe(vehicle.getMarca()), nullSafe(vehicle.getModelo())).trim();
        if (vehicle.getMatricula() != null && !vehicle.getMatricula().isBlank()) {
            return label + " - " + vehicle.getMatricula();
        }
        return label;
    }

    private String formatLocation(Empresa empresa) {
        if (empresa == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (empresa.getCp() != null) {
            sb.append(empresa.getCp()).append(" ");
        }
        if (empresa.getCiudad() != null) {
            sb.append(empresa.getCiudad());
        }
        if (empresa.getProvincia() != null) {
            if (!sb.isEmpty()) {
                sb.append(" - ");
            }
            sb.append(empresa.getProvincia());
        }
        return sb.toString().trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
