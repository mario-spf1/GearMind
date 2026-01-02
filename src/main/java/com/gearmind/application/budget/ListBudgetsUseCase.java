package com.gearmind.application.budget;

import com.gearmind.domain.budget.Budget;
import com.gearmind.domain.budget.BudgetRepository;
import java.util.List;

public class ListBudgetsUseCase {

    private final BudgetRepository budgetRepository;

    public ListBudgetsUseCase(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    public List<Budget> listAllWithEmpresa() {
        return budgetRepository.findAllWithEmpresa();
    }

    public List<Budget> listByEmpresa(long empresaId) {
        return budgetRepository.findByEmpresaId(empresaId);
    }
}
