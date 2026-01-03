package com.gearmind.application.invoice;

import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceLine;
import com.gearmind.domain.invoice.InvoiceRepository;
import com.gearmind.domain.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SaveInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;

    public SaveInvoiceUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Invoice execute(SaveInvoiceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La petición de factura no puede ser nula.");
        }
        if (request.getEmpresaId() == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (request.getClienteId() == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        if (request.getVehiculoId() == null) {
            throw new IllegalArgumentException("El vehículo es obligatorio.");
        }
        if (request.getPresupuestoId() == null) {
            throw new IllegalArgumentException("El presupuesto es obligatorio.");
        }
        if (request.getLineas() == null || request.getLineas().isEmpty()) {
            throw new IllegalArgumentException("La factura debe incluir al menos una línea.");
        }

        List<InvoiceLine> normalizedLines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (InvoiceLine line : request.getLineas()) {
            if (line.getDescripcion() == null || line.getDescripcion().isBlank()) {
                throw new IllegalArgumentException("Todas las líneas deben tener descripción.");
            }
            if (line.getCantidad() == null || line.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
            }
            if (line.getPrecio() == null || line.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El precio no puede ser negativo.");
            }

            InvoiceLine normalized = new InvoiceLine();
            normalized.setId(line.getId());
            normalized.setProductoId(line.getProductoId());
            normalized.setDescripcion(line.getDescripcion().trim());
            normalized.setCantidad(line.getCantidad());
            normalized.setPrecio(line.getPrecio());

            BigDecimal lineTotal = line.getCantidad().multiply(line.getPrecio());
            normalized.setTotal(lineTotal);
            subtotal = subtotal.add(lineTotal);
            normalizedLines.add(normalized);
        }

        int ivaPercent = request.getIvaPercent() != null ? request.getIvaPercent() : 21;
        BigDecimal iva = subtotal.multiply(BigDecimal.valueOf(ivaPercent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva);

        Invoice invoice = new Invoice();
        invoice.setId(request.getId());
        invoice.setEmpresaId(request.getEmpresaId());
        invoice.setClienteId(request.getClienteId());
        invoice.setVehiculoId(request.getVehiculoId());
        invoice.setPresupuestoId(request.getPresupuestoId());
        invoice.setNumero(request.getNumero() != null ? request.getNumero() : generateNumber());
        invoice.setFecha(LocalDateTime.now());
        invoice.setEstado(request.getEstado() != null ? request.getEstado() : InvoiceStatus.PENDIENTE);
        invoice.setObservaciones(request.getObservaciones());
        invoice.setSubtotal(subtotal);
        invoice.setIva(iva);
        invoice.setTotal(total);
        invoice.setUpdatedAt(LocalDateTime.now());
        if (request.getId() == null) {
            invoice.setCreatedAt(LocalDateTime.now());
        }

        return invoiceRepository.save(invoice, normalizedLines);
    }

    private String generateNumber() {
        return "F-" + System.currentTimeMillis();
    }
}
