package com.gearmind.application.payment;

import com.gearmind.domain.payment.PaymentRecord;
import com.gearmind.domain.payment.PaymentRepository;

import java.util.List;

public class ListPaymentRecordsUseCase {

    private final PaymentRepository paymentRepository;

    public ListPaymentRecordsUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentRecord> execute(long pagoId) {
        return paymentRepository.findRecords(pagoId);
    }
}
