package com.gearmind.application.payment;

import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceRepository;
import com.gearmind.domain.invoice.InvoiceStatus;
import com.gearmind.domain.payment.Payment;
import com.gearmind.domain.payment.PaymentInstallment;
import com.gearmind.domain.payment.PaymentInstallmentStatus;
import com.gearmind.domain.payment.PaymentRecord;
import com.gearmind.domain.payment.PaymentRepository;
import com.gearmind.domain.payment.PaymentStatus;
import com.gearmind.domain.payment.PaymentType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    public CreatePaymentUseCase(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public long execute(CreatePaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de pago no puede ser nula.");
        }

        if (request.getTipo() == null) {
            throw new IllegalArgumentException("El tipo de pago es obligatorio.");
        }

        Invoice invoice = invoiceRepository.findById(request.getFacturaId()).orElseThrow(() -> new IllegalArgumentException("No se encontró la factura indicada."));

        if (invoice.getEstado() == InvoiceStatus.ANULADA) {
            throw new IllegalArgumentException("No se puede registrar un pago sobre una factura anulada.");
        }

        paymentRepository.findByFacturaId(request.getFacturaId()).ifPresent(existing -> {
            throw new IllegalStateException("La factura ya tiene un pago registrado.");
        });

        Payment payment = new Payment();
        payment.setEmpresaId(invoice.getEmpresaId());
        payment.setFacturaId(invoice.getId());
        payment.setClienteId(invoice.getClienteId());
        payment.setTipo(request.getTipo());
        payment.setTotal(invoice.getTotal());

        List<PaymentInstallment> installments = new ArrayList<>();
        List<PaymentRecord> records = new ArrayList<>();

        if (request.getTipo() == PaymentType.CONTADO) {
            payment.setEstado(PaymentStatus.COMPLETADO);
            payment.setNumeroPlazos(1);
            PaymentInstallment installment = new PaymentInstallment();
            installment.setNumero(1);
            installment.setFechaVencimiento(LocalDate.now());
            installment.setImporte(invoice.getTotal());
            installment.setEstado(PaymentInstallmentStatus.PAGADO);
            installment.setFechaPago(LocalDateTime.now());
            installments.add(installment);

            PaymentRecord record = new PaymentRecord();
            record.setFecha(LocalDateTime.now());
            record.setImporte(invoice.getTotal());
            record.setObservaciones("Pago al contado");
            records.add(record);
        } else {
            if (request.getNumeroPlazos() <= 0) {
                throw new IllegalArgumentException("El número de plazos debe ser mayor que cero.");
            }
            if (request.getPrimerVencimiento() == null) {
                throw new IllegalArgumentException("Debes indicar el primer vencimiento.");
            }
            payment.setEstado(PaymentStatus.ABIERTO);
            payment.setNumeroPlazos(request.getNumeroPlazos());
            installments.addAll(buildInstallments(invoice.getTotal(), request.getNumeroPlazos(), request.getPrimerVencimiento()));
        }

        long paymentId = paymentRepository.createPayment(payment, installments, records);

        if (payment.getEstado() == PaymentStatus.COMPLETADO) {
            invoiceRepository.updateStatus(invoice.getId(), InvoiceStatus.PAGADA);
        }

        return paymentId;
    }

    private List<PaymentInstallment> buildInstallments(BigDecimal total, int count, LocalDate firstDueDate) {
        List<PaymentInstallment> installments = new ArrayList<>();
        BigDecimal totalBase = total == null ? BigDecimal.ZERO : total;
        BigDecimal baseAmount = totalBase.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal remainder = totalBase.subtract(baseAmount.multiply(BigDecimal.valueOf(count)));

        for (int i = 0; i < count; i++) {
            PaymentInstallment installment = new PaymentInstallment();
            installment.setNumero(i + 1);
            installment.setFechaVencimiento(firstDueDate.plusMonths(i));
            BigDecimal amount = baseAmount;
            if (i == count - 1) {
                amount = amount.add(remainder);
            }
            installment.setImporte(amount);
            installment.setEstado(PaymentInstallmentStatus.PENDIENTE);
            installments.add(installment);
        }

        return installments;
    }
}
