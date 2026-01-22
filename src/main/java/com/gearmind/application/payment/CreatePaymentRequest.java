package com.gearmind.application.payment;

import com.gearmind.domain.payment.PaymentType;

import java.time.LocalDate;

public class CreatePaymentRequest {

    private long facturaId;
    private PaymentType tipo;
    private int numeroPlazos;
    private LocalDate primerVencimiento;

    public long getFacturaId() {
        return facturaId;
    }

    public void setFacturaId(long facturaId) {
        this.facturaId = facturaId;
    }

    public PaymentType getTipo() {
        return tipo;
    }

    public void setTipo(PaymentType tipo) {
        this.tipo = tipo;
    }

    public int getNumeroPlazos() {
        return numeroPlazos;
    }

    public void setNumeroPlazos(int numeroPlazos) {
        this.numeroPlazos = numeroPlazos;
    }

    public LocalDate getPrimerVencimiento() {
        return primerVencimiento;
    }

    public void setPrimerVencimiento(LocalDate primerVencimiento) {
        this.primerVencimiento = primerVencimiento;
    }
}
