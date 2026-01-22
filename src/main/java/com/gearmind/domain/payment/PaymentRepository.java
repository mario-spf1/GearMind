package com.gearmind.domain.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    List<Payment> findAllWithEmpresa();

    List<Payment> findByEmpresaId(long empresaId);

    Optional<Payment> findById(long pagoId);

    Optional<Payment> findByFacturaId(long facturaId);

    long createPayment(Payment payment, List<PaymentInstallment> installments, List<PaymentRecord> records);

    List<PaymentInstallment> findInstallments(long pagoId);

    Optional<PaymentInstallment> findInstallmentById(long installmentId);

    List<PaymentRecord> findRecords(long pagoId);

    void registerInstallmentPayment(long pagoId, long installmentId, BigDecimal amount, LocalDateTime fechaPago, String observaciones);

    int countPendingInstallments(long pagoId);

    void updatePaymentStatus(long pagoId, PaymentStatus status);
}
