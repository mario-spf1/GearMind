package com.gearmind.application.budget;

import com.gearmind.domain.budget.Budget;
import com.gearmind.domain.budget.BudgetRepository;

import java.util.Optional;

public class GetBudgetUseCase {

    private final BudgetRepository budgetRepository;

    public GetBudgetUseCase(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    public Optional<Budget> execute(long id) {
        return budgetRepository.findById(id);
    }
}
