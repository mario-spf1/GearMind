package com.gearmind.infrastructure.invoice;

import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceLine;
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

public class InvoicePdfGenerator {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(new Locale("es", "ES")));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color META_BACKGROUND = new Color(246, 247, 251);
    private static final Color META_BORDER = new Color(221, 225, 232);
    private static final Color HEADER_BACKGROUND = new Color(38, 44, 58);
    private static final Color ROW_ALT_BACKGROUND = new Color(248, 250, 253);

    public Path generate(Invoice invoice, List<InvoiceLine> lines, Empresa empresa, Customer customer, Vehicle vehicle) {
        try {
            Path baseDir = InvoicePdfStorage.baseDir();
            Files.createDirectories(baseDir);
            Path outputPath = InvoicePdfStorage.resolvePath(invoice.getId());
            Document document = new Document(PageSize.A4, 36, 36, 48, 36);
            PdfWriter.getInstance(document, new FileOutputStream(outputPath.toFile()));
            document.open();
            addHeader(document, invoice, empresa);
            addCustomerSection(document, customer, vehicle);
            addLinesTable(document, lines);
            addTotals(document, invoice);
            addFooter(document, invoice);
            document.close();
            return outputPath;
        } catch (Exception e) {
            throw new RuntimeException("Error generando el PDF de la factura", e);
        }
    }

    private void addHeader(Document document, Invoice invoice, Empresa empresa) throws DocumentException {
        Font documentTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
        Paragraph documentTitle = new Paragraph("FACTURA", documentTitleFont);
        documentTitle.setAlignment(Element.ALIGN_CENTER);
        documentTitle.setSpacingAfter(12);
        document.add(documentTitle);
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{60, 40});
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        Paragraph companyName = new Paragraph(empresa != null ? empresa.getNombre() : "GearMind", titleFont);
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
        right.addElement(buildMetaBox(invoice));
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
        PdfPCell clienteCell = cardCell();
        clienteCell.addElement(new Paragraph("Cliente", labelFont));
        if (customer != null) {
            clienteCell.addElement(new Paragraph(customer.getNombre(), textFont));
            if (customer.getDni() != null && !customer.getDni().isBlank()) {
                clienteCell.addElement(new Paragraph("DNI: " + customer.getDni(), textFont));
            }

            if (customer.getEmail() != null) {
                clienteCell.addElement(new Paragraph(customer.getEmail(), textFont));
            }
            if (customer.getTelefono() != null) {
                clienteCell.addElement(new Paragraph("Tel: " + customer.getTelefono(), textFont));
            }
        }

        PdfPCell vehiculoCell = cardCell();
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

    private void addLinesTable(Document document, List<InvoiceLine> lines) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{46, 10, 14, 12, 18});
        addHeaderCell(table, "Concepto");
        addHeaderCell(table, "Cant.");
        addHeaderCell(table, "Precio");
        addHeaderCell(table, "Dto %");
        addHeaderCell(table, "Importe");
        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        int index = 0;
        for (InvoiceLine line : lines) {
            boolean altRow = index % 2 == 1;
            table.addCell(bodyCell(line.getDescripcion(), rowFont, altRow, Element.ALIGN_LEFT));
            table.addCell(bodyCell(formatDecimal(line.getCantidad()), rowFont, altRow, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(formatMoney(line.getPrecio()), rowFont, altRow, Element.ALIGN_RIGHT));
            table.addCell(bodyCell("0%", rowFont, altRow, Element.ALIGN_RIGHT));
            table.addCell(bodyCell(formatMoney(line.getTotal()), rowFont, altRow, Element.ALIGN_RIGHT));
            index++;
        }

        table.setSpacingAfter(10);
        document.add(table);
    }

    private void addTotals(Document document, Invoice invoice) throws DocumentException {
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(42);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setWidths(new float[]{60, 40});
        totals.addCell(totalRow("Subtotal", false));
        totals.addCell(totalRowValue(invoice.getSubtotal(), false));
        totals.addCell(totalRow(ivaLabel(invoice), false));
        totals.addCell(totalRowValue(invoice.getIva(), false));
        totals.addCell(totalRow("TOTAL", true));
        totals.addCell(totalRowValue(invoice.getTotal(), true));
        totals.setSpacingAfter(12);
        document.add(totals);
    }

    private void addFooter(Document document, Invoice invoice) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        String observaciones = invoice.getObservaciones();
        Paragraph obsTitle = new Paragraph("Observaciones", titleFont);
        Paragraph obsText = new Paragraph(observaciones == null || observaciones.isBlank() ? "—" : observaciones, textFont);
        document.add(obsTitle);
        document.add(obsText);
        Paragraph condicionesTitle = new Paragraph("Condiciones de pago", titleFont);
        Paragraph condicionesText = new Paragraph("Forma de pago: transferencia bancaria o tarjeta. Gracias por su confianza.", textFont);
        condicionesTitle.setSpacingBefore(8);
        document.add(condicionesTitle);
        document.add(condicionesText);

        PdfPTable firma = new PdfPTable(2);
        firma.setWidthPercentage(100);
        firma.setSpacingBefore(18);
        firma.setWidths(new float[]{50, 50});
        firma.addCell(signatureCell("Firma / sello cliente"));
        firma.addCell(signatureCell("Firma / sello empresa"));
        document.add(firma);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BACKGROUND);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0,00 €";
        }
        return MONEY_FORMAT.format(value) + " €";
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatStatus(Invoice invoice) {
        if (invoice.getEstado() == null) {
            return "Pendiente";
        }
        return switch (invoice.getEstado()) {
            case BORRADOR ->
                "Borrador";
            case PENDIENTE ->
                "Pendiente";
            case PAGADA ->
                "Pagada";
            case ANULADA ->
                "Anulada";
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

    private PdfPTable buildMetaBox(Invoice invoice) throws DocumentException {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(90, 96, 110));
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(100);
        meta.setWidths(new float[]{40, 60});
        meta.addCell(metaCell("Nº", labelFont));
        meta.addCell(metaCell(formatInvoiceNumber(invoice), valueFont));
        meta.addCell(metaCell("Fecha", labelFont));
        meta.addCell(metaCell(invoice.getFecha() != null ? invoice.getFecha().format(DATE_FORMAT) : "—", valueFont));
        meta.addCell(metaCell("Estado", labelFont));
        meta.addCell(metaCell(formatStatus(invoice), valueFont));

        PdfPCell container = new PdfPCell(meta);
        container.setPadding(8);
        container.setBackgroundColor(META_BACKGROUND);
        container.setBorderColor(META_BORDER);
        container.setBorderWidth(1f);

        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.addCell(container);
        return wrapper;
    }

    private PdfPCell metaCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private PdfPCell cardCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(META_BACKGROUND);
        cell.setBorderColor(META_BORDER);
        cell.setBorderWidth(1f);
        cell.setPadding(10);
        return cell;
    }

    private PdfPCell bodyCell(String text, Font font, boolean altRow, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        if (altRow) {
            cell.setBackgroundColor(ROW_ALT_BACKGROUND);
        }
        return cell;
    }

    private PdfPCell totalRow(String label, boolean highlight) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, highlight ? 12 : 10);
        PdfPCell cell = new PdfPCell(new Phrase(label, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (highlight) {
            cell.setBackgroundColor(META_BACKGROUND);
            cell.setPadding(6);
        }
        return cell;
    }

    private PdfPCell totalRowValue(BigDecimal value, boolean highlight) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, highlight ? 12 : 10);
        PdfPCell cell = new PdfPCell(new Phrase(formatMoney(value), font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (highlight) {
            cell.setBackgroundColor(META_BACKGROUND);
            cell.setPadding(6);
        }
        return cell;
    }

    private PdfPCell signatureCell(String label) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
        PdfPCell cell = new PdfPCell(new Phrase(label, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(18);
        cell.setPaddingBottom(10);
        cell.setBorderWidthTop(1f);
        cell.setBorderColorTop(META_BORDER);
        return cell;
    }

    private String ivaLabel(Invoice invoice) {
        BigDecimal subtotal = invoice.getSubtotal();
        BigDecimal iva = invoice.getIva();
        if (subtotal == null || iva == null || subtotal.compareTo(BigDecimal.ZERO) == 0) {
            return "IVA";
        }
        BigDecimal rate = iva.multiply(new BigDecimal("100")).divide(subtotal, 0, java.math.RoundingMode.HALF_UP);
        return "IVA (" + rate.toPlainString() + "%)";
    }

    private String formatInvoiceNumber(Invoice invoice) {
        if (invoice.getNumero() != null && !invoice.getNumero().isBlank()) {
            return invoice.getNumero();
        }
        if (invoice.getId() == null) {
            return "—";
        }
        return String.format("%06d", invoice.getId());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
