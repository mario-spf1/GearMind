package com.gearmind.application.invoice;

import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.invoice.InvoiceRepository;
import com.gearmind.infrastructure.invoice.InvoicePdfStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeleteInvoiceUseCase {

    private final InvoiceRepository repository;

    public DeleteInvoiceUseCase(InvoiceRepository repository) {
        this.repository = repository;
    }

    public void delete(long invoiceId, long empresaId) {
        if (!AuthContext.isAdminOrSuperAdmin()) {
            throw new IllegalStateException("No tienes permisos para eliminar facturas.");
        }
        repository.delete(invoiceId, empresaId);
        deletePdf(invoiceId);
    }

    private void deletePdf(long invoiceId) {
        Path path = InvoicePdfStorage.resolvePath(invoiceId);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Error eliminando el PDF de la factura", e);
        }
    }
}
