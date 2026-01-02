package com.gearmind.domain.budget;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository {

    List<Budget> findAllWithEmpresa();

    List<Budget> findByEmpresaId(long empresaId);

    Optional<Budget> findById(long id);

    List<BudgetLine> findLinesByBudgetId(long budgetId);

    Budget save(Budget budget, List<BudgetLine> lines);
}
