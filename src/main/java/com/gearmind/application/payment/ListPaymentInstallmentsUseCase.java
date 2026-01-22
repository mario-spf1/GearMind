package com.gearmind.application.payment;

import com.gearmind.domain.payment.PaymentInstallment;
import com.gearmind.domain.payment.PaymentRepository;

import java.util.List;

public class ListPaymentInstallmentsUseCase {

    private final PaymentRepository paymentRepository;

    public ListPaymentInstallmentsUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentInstallment> execute(long pagoId) {
        return paymentRepository.findInstallments(pagoId);
    }
}
