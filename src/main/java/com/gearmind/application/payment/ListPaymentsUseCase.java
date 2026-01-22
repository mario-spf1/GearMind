package com.gearmind.application.payment;

import com.gearmind.domain.payment.Payment;
import com.gearmind.domain.payment.PaymentRepository;

import java.util.List;

public class ListPaymentsUseCase {

    private final PaymentRepository paymentRepository;

    public ListPaymentsUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> listAllWithEmpresa() {
        return paymentRepository.findAllWithEmpresa();
    }

    public List<Payment> listByEmpresa(long empresaId) {
        return paymentRepository.findByEmpresaId(empresaId);
    }
}
