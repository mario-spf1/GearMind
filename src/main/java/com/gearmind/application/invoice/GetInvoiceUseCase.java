package com.gearmind.application.invoice;

import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceRepository;

import java.util.Optional;

public class GetInvoiceUseCase {

    private final InvoiceRepository invoiceRepository;

    public GetInvoiceUseCase(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Optional<Invoice> execute(long id) {
        return invoiceRepository.findById(id);
    }
}
