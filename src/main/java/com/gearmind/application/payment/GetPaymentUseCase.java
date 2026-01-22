package com.gearmind.application.payment;

import com.gearmind.domain.payment.Payment;
import com.gearmind.domain.payment.PaymentRepository;

import java.util.Optional;

public class GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Optional<Payment> execute(long pagoId) {
        return paymentRepository.findById(pagoId);
    }
}
