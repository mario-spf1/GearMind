package com.gearmind.application.budget;

import com.gearmind.domain.budget.Budget;
import com.gearmind.domain.budget.BudgetLine;
import com.gearmind.domain.budget.BudgetRepository;
import com.gearmind.domain.budget.BudgetStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SaveBudgetUseCase {

    private final BudgetRepository budgetRepository;

    public SaveBudgetUseCase(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
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

        return budgetRepository.save(budget, normalizedLines);
    }
}
