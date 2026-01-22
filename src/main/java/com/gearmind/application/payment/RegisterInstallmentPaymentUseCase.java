package com.gearmind.application.payment;

import com.gearmind.domain.invoice.InvoiceRepository;
import com.gearmind.domain.invoice.InvoiceStatus;
import com.gearmind.domain.payment.Payment;
import com.gearmind.domain.payment.PaymentInstallment;
import com.gearmind.domain.payment.PaymentInstallmentStatus;
import com.gearmind.domain.payment.PaymentRepository;
import com.gearmind.domain.payment.PaymentStatus;

import java.time.LocalDateTime;

public class RegisterInstallmentPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    public RegisterInstallmentPaymentUseCase(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public void execute(long paymentId, long installmentId, String observaciones) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("No se encontró el pago indicado."));

        if (payment.getEstado() == PaymentStatus.COMPLETADO) {
            throw new IllegalStateException("El pago ya está completado.");
        }

        PaymentInstallment installment = paymentRepository.findInstallmentById(installmentId).orElseThrow(() -> new IllegalArgumentException("No se encontró el plazo indicado."));

        if (installment.getEstado() == PaymentInstallmentStatus.PAGADO) {
            throw new IllegalStateException("El plazo ya está pagado.");
        }

        paymentRepository.registerInstallmentPayment(paymentId, installmentId, installment.getImporte(), LocalDateTime.now(), observaciones);

        if (paymentRepository.countPendingInstallments(paymentId) == 0) {
            paymentRepository.updatePaymentStatus(paymentId, PaymentStatus.COMPLETADO);
            invoiceRepository.updateStatus(payment.getFacturaId(), InvoiceStatus.PAGADA);
        }
    }
}
