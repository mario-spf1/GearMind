package com.gearmind.application.budget;

import com.gearmind.domain.budget.Budget;
import com.gearmind.domain.budget.BudgetLine;
import com.gearmind.domain.budget.BudgetRepository;
import com.gearmind.domain.budget.BudgetStatus;
import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.company.EmpresaRepository;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.domain.vehicle.VehicleRepository;
import com.gearmind.infrastructure.budget.BudgetPdfGenerator;
import com.gearmind.application.telegram.SendTelegramNotificationUseCase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaveBudgetUseCase {

    private final BudgetRepository budgetRepository;
    private final EmpresaRepository empresaRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final BudgetPdfGenerator pdfGenerator;
    private final SendTelegramNotificationUseCase notificationUseCase;

    public SaveBudgetUseCase(BudgetRepository budgetRepository, EmpresaRepository empresaRepository, CustomerRepository customerRepository, VehicleRepository vehicleRepository, BudgetPdfGenerator pdfGenerator) {
        this.budgetRepository = budgetRepository;
        this.empresaRepository = empresaRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.pdfGenerator = pdfGenerator;
        this.notificationUseCase = null;
    }

    public SaveBudgetUseCase(BudgetRepository budgetRepository, EmpresaRepository empresaRepository, CustomerRepository customerRepository, VehicleRepository vehicleRepository, BudgetPdfGenerator pdfGenerator, SendTelegramNotificationUseCase notificationUseCase) {
        this.budgetRepository = budgetRepository;
        this.empresaRepository = empresaRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.pdfGenerator = pdfGenerator;
        this.notificationUseCase = notificationUseCase;
    }

    public Budget execute(SaveBudgetRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La petición de presupuesto no puede ser nula.");
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
        if (request.getLineas() == null || request.getLineas().isEmpty()) {
            throw new IllegalArgumentException("El presupuesto debe incluir al menos una línea.");
        }

        List<BudgetLine> normalizedLines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (BudgetLine line : request.getLineas()) {
            if (line.getDescripcion() == null || line.getDescripcion().isBlank()) {
                throw new IllegalArgumentException("Todas las líneas deben tener descripción.");
            }
            if (line.getCantidad() == null || line.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
            }
            if (line.getPrecio() == null || line.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El precio no puede ser negativo.");
            }

            BudgetLine normalized = new BudgetLine();
            normalized.setId(line.getId());
            normalized.setProductoId(line.getProductoId());
            normalized.setDescripcion(line.getDescripcion().trim());
            normalized.setCantidad(line.getCantidad());
            normalized.setPrecio(line.getPrecio());
            BigDecimal lineTotal = line.getCantidad().multiply(line.getPrecio());
            normalized.setTotal(lineTotal);
            total = total.add(lineTotal);
            normalizedLines.add(normalized);
        }

        BudgetStatus previousStatus = null;
        if (request.getId() != null) {
            Optional<Budget> existing = budgetRepository.findById(request.getId());
            if (existing.isPresent()) {
                previousStatus = existing.get().getEstado();
            }
        }

        Budget budget = new Budget();
        budget.setId(request.getId());
        budget.setEmpresaId(request.getEmpresaId());
        budget.setClienteId(request.getClienteId());
        budget.setVehiculoId(request.getVehiculoId());
        budget.setReparacionId(request.getReparacionId());
        budget.setFecha(LocalDateTime.now());
        budget.setEstado(request.getEstado() != null ? request.getEstado() : BudgetStatus.BORRADOR);
        budget.setObservaciones(request.getObservaciones());
        budget.setTotalEstimado(total);
        budget.setUpdatedAt(LocalDateTime.now());
        if (request.getId() == null) {
            budget.setCreatedAt(LocalDateTime.now());
        }

        Budget saved = budgetRepository.save(budget, normalizedLines);
        generatePdf(saved, normalizedLines);
        notifyEmission(saved, previousStatus, request.getId() == null);
        return saved;
    }

    private void generatePdf(Budget budget, List<BudgetLine> lines) {
        Empresa empresa = empresaRepository.findById(budget.getEmpresaId()).orElse(null);
        Customer customer = customerRepository.findById(budget.getClienteId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(budget.getVehiculoId()).orElse(null);
        pdfGenerator.generate(budget, lines, empresa, customer, vehicle);
    }

    private void notifyEmission(Budget budget, BudgetStatus previousStatus, boolean isNew) {
        if (notificationUseCase == null || budget.getClienteId() == null) {
            return;
        }
        if (budget.getEstado() != BudgetStatus.ENVIADO) {
            return;
        }
        if (!isNew && previousStatus == BudgetStatus.ENVIADO) {
            return;
        }
        String message = "Se ha emitido tu presupuesto #" + budget.getId() + " con un importe estimado de " + budget.getTotalEstimado() + ".";
        String payload = "budgetId=" + budget.getId() + ";status=" + budget.getEstado().name();
        notificationUseCase.execute(budget.getClienteId(), "PRESUPUESTO_EMITIDO", message, payload);
    }
}
