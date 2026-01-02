package com.gearmind.infrastructure.budget;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class BudgetPdfStorage {

    private BudgetPdfStorage() {
    }

    public static Path resolvePath(long budgetId) {
        return baseDir().resolve(String.format("presupuesto_%06d.pdf", budgetId));
    }

    public static Path baseDir() {
        return Paths.get(System.getProperty("user.home"), "GearMind", "presupuestos");
    }
}
