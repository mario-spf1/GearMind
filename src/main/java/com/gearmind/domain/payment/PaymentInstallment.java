package com.gearmind.domain.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaymentInstallment {

    private Long id;
    private Long pagoId;
    private Integer numero;
    private LocalDate fechaVencimiento;
    private BigDecimal importe;
    private PaymentInstallmentStatus estado;
    private LocalDateTime fechaPago;

    public PaymentInstallment() {
    }

    public PaymentInstallment(Long id, Long pagoId, Integer numero, LocalDate fechaVencimiento, BigDecimal importe, PaymentInstallmentStatus estado, LocalDateTime fechaPago) {
        this.id = id;
        this.pagoId = pagoId;
        this.numero = numero;
        this.fechaVencimiento = fechaVencimiento;
        this.importe = importe;
        this.estado = estado;
        this.fechaPago = fechaPago;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPagoId() {
        return pagoId;
    }

    public void setPagoId(Long pagoId) {
        this.pagoId = pagoId;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public PaymentInstallmentStatus getEstado() {
        return estado;
    }

    public void setEstado(PaymentInstallmentStatus estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }
}
