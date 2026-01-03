package com.gearmind.domain.invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {

    List<Invoice> findAllWithEmpresa();

    List<Invoice> findByEmpresaId(long empresaId);

    Optional<Invoice> findById(long id);

    List<InvoiceLine> findLinesByInvoiceId(long invoiceId);

    Invoice save(Invoice invoice, List<InvoiceLine> lines);
}
