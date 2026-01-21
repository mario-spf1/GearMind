package com.gearmind.application.invoice;

import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceLine;
import com.gearmind.domain.invoice.InvoiceRepository;
import com.gearmind.domain.invoice.InvoiceStatus;
import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.company.EmpresaRepository;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.domain.vehicle.VehicleRepository;
import com.gearmind.infrastructure.invoice.InvoicePdfGenerator;
import com.gearmind.application.inventory.AdjustInventoryUseCase;
import com.gearmind.application.telegram.SendTelegramNotificationUseCase;
import com.gearmind.domain.inventory.InventoryMovementType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SaveInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;
    private final EmpresaRepository empresaRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final InvoicePdfGenerator pdfGenerator;
    private final SendTelegramNotificationUseCase notificationUseCase;
    private final AdjustInventoryUseCase adjustInventoryUseCase;

    public SaveInvoiceUseCase(InvoiceRepository invoiceRepository, EmpresaRepository empresaRepository, CustomerRepository customerRepository, VehicleRepository vehicleRepository, InvoicePdfGenerator pdfGenerator) {
        this.invoiceRepository = invoiceRepository;
        this.empresaRepository = empresaRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.pdfGenerator = pdfGenerator;
        this.notificationUseCase = null;
        this.adjustInventoryUseCase = null;
    }

    public SaveInvoiceUseCase(InvoiceRepository invoiceRepository, EmpresaRepository empresaRepository, CustomerRepository customerRepository, VehicleRepository vehicleRepository, InvoicePdfGenerator pdfGenerator, SendTelegramNotificationUseCase notificationUseCase) {
        this.invoiceRepository = invoiceRepository;
        this.empresaRepository = empresaRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.pdfGenerator = pdfGenerator;
        this.notificationUseCase = notificationUseCase;
        this.adjustInventoryUseCase = null;
    }

    public SaveInvoiceUseCase(InvoiceRepository invoiceRepository, EmpresaRepository empresaRepository, CustomerRepository customerRepository, VehicleRepository vehicleRepository, InvoicePdfGenerator pdfGenerator, SendTelegramNotificationUseCase notificationUseCase, AdjustInventoryUseCase adjustInventoryUseCase) {
        this.invoiceRepository = invoiceRepository;
        this.empresaRepository = empresaRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.pdfGenerator = pdfGenerator;
        this.notificationUseCase = notificationUseCase;
        this.adjustInventoryUseCase = adjustInventoryUseCase;
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
        if (request.getPresupuestoId() == null) {
            throw new IllegalArgumentException("El presupuesto es obligatorio.");
        }
        if (request.getLineas() == null || request.getLineas().isEmpty()) {
            throw new IllegalArgumentException("La factura debe incluir al menos una línea.");
        }

        List<InvoiceLine> normalizedLines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        List<InvoiceLine> previousLines = List.of();

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

        InvoiceStatus previousStatus = null;
        if (request.getId() != null) {
            Optional<Invoice> existing = invoiceRepository.findById(request.getId());
            if (existing.isPresent()) {
                previousStatus = existing.get().getEstado();
            }
            previousLines = invoiceRepository.findLinesByInvoiceId(request.getId());
        }

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

        Invoice saved = invoiceRepository.save(invoice, normalizedLines);
        updateInventoryMovements(saved, previousLines, normalizedLines);
        generatePdf(saved, normalizedLines);
        notifyEmission(saved, previousStatus, request.getId() == null);
        return saved;
    }

    private String generateNumber() {
        return "F-" + System.currentTimeMillis();
    }

    private void generatePdf(Invoice invoice, List<InvoiceLine> lines) {
        Empresa empresa = empresaRepository.findById(invoice.getEmpresaId()).orElse(null);
        Customer customer = customerRepository.findById(invoice.getClienteId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(invoice.getVehiculoId()).orElse(null);
        pdfGenerator.generate(invoice, lines, empresa, customer, vehicle);
    }

    private void updateInventoryMovements(Invoice invoice, List<InvoiceLine> previousLines, List<InvoiceLine> newLines) {
        if (adjustInventoryUseCase == null) {
            return;
        }
        Map<Long, Integer> previousTotals = aggregateProductQuantities(previousLines);
        Map<Long, Integer> newTotals = aggregateProductQuantities(newLines);

        for (Map.Entry<Long, Integer> entry : newTotals.entrySet()) {
            long productId = entry.getKey();
            int newQty = entry.getValue();
            int oldQty = previousTotals.getOrDefault(productId, 0);
            int delta = newQty - oldQty;
            applyInventoryDelta(invoice, productId, delta);
        }

        for (Map.Entry<Long, Integer> entry : previousTotals.entrySet()) {
            long productId = entry.getKey();
            if (newTotals.containsKey(productId)) {
                continue;
            }
            int delta = -entry.getValue();
            applyInventoryDelta(invoice, productId, delta);
        }
    }

    private Map<Long, Integer> aggregateProductQuantities(List<InvoiceLine> lines) {
        Map<Long, Integer> totals = new HashMap<>();
        if (lines == null) {
            return totals;
        }
        for (InvoiceLine line : lines) {
            if (line.getProductoId() == null) {
                continue;
            }
            int qty = toIntQuantity(line.getCantidad());
            totals.merge(line.getProductoId(), qty, Integer::sum);
        }
        return totals;
    }

    private int toIntQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return 0;
        }
        try {
            return quantity.intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("La cantidad de producto debe ser un número entero.");
        }
    }

    private void applyInventoryDelta(Invoice invoice, long productId, int delta) {
        if (delta == 0) {
            return;
        }
        InventoryMovementType type = delta > 0 ? InventoryMovementType.SALIDA : InventoryMovementType.ENTRADA;
        int quantity = Math.abs(delta);
        String reference = invoice.getNumero() == null ? "Factura #" + invoice.getId() : "Factura " + invoice.getNumero();
        String notas = delta > 0 ? "Consumo en factura" : "Ajuste por edición de factura";
        adjustInventoryUseCase.execute(invoice.getEmpresaId(), productId, type, quantity, reference, notas);
    }

    private void notifyEmission(Invoice invoice, InvoiceStatus previousStatus, boolean isNew) {
        if (notificationUseCase == null || invoice.getClienteId() == null) {
            return;
        }
        if (invoice.getEstado() != InvoiceStatus.PENDIENTE) {
            return;
        }
        if (!isNew && previousStatus == InvoiceStatus.PENDIENTE) {
            return;
        }
        String message = "Se ha emitido tu factura " + invoice.getNumero() + " por un total de " + invoice.getTotal() + ".";
        String payload = "invoiceId=" + invoice.getId() + ";status=" + invoice.getEstado().name();
        notificationUseCase.execute(invoice.getClienteId(), "FACTURA_EMITIDA", message, payload);
    }
}
