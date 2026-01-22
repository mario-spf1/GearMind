package com.gearmind.domain.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentRecord {

    private Long id;
    private Long pagoId;
    private Long plazoId;
    private LocalDateTime fecha;
    private BigDecimal importe;
    private String observaciones;

    public PaymentRecord() {
    }

    public PaymentRecord(Long id, Long pagoId, Long plazoId, LocalDateTime fecha, BigDecimal importe, String observaciones) {
        this.id = id;
        this.pagoId = pagoId;
        this.plazoId = plazoId;
        this.fecha = fecha;
        this.importe = importe;
        this.observaciones = observaciones;
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

    public Long getPlazoId() {
        return plazoId;
    }

    public void setPlazoId(Long plazoId) {
        this.plazoId = plazoId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
