package com.gearmind.application.budget;

import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.budget.BudgetRepository;
import com.gearmind.infrastructure.budget.BudgetPdfStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeleteBudgetUseCase {

    private final BudgetRepository repository;

    public DeleteBudgetUseCase(BudgetRepository repository) {
        this.repository = repository;
    }

    public void delete(long budgetId, long empresaId) {
        if (!AuthContext.isAdminOrSuperAdmin()) {
            throw new IllegalStateException("No tienes permisos para eliminar presupuestos.");
        }
        repository.delete(budgetId, empresaId);
        deletePdf(budgetId);
    }

    private void deletePdf(long budgetId) {
        Path path = BudgetPdfStorage.resolvePath(budgetId);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Error eliminando el PDF del presupuesto", e);
        }
    }
}
