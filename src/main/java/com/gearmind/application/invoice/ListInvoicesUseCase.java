package com.gearmind.application.invoice;

import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceRepository;

import java.util.List;

public class ListInvoicesUseCase {

    private final InvoiceRepository invoiceRepository;

    public ListInvoicesUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<Invoice> listAllWithEmpresa() {
        return invoiceRepository.findAllWithEmpresa();
    }

    public List<Invoice> listByEmpresa(long empresaId) {
        return invoiceRepository.findByEmpresaId(empresaId);
    }
}
