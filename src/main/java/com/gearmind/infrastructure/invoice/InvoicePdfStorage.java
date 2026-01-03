package com.gearmind.infrastructure.invoice;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class InvoicePdfStorage {

    private InvoicePdfStorage() {
    }

    public static Path resolvePath(long invoiceId) {
        return baseDir().resolve(String.format("factura_%06d.pdf", invoiceId));
    }

    public static Path baseDir() {
        return Paths.get(System.getProperty("user.home"), "GearMind", "facturas");
    }
}
