package com.gearmind.domain.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {

    private Long id;
    private Long empresaId;
    private Long facturaId;
    private Long clienteId;
    private PaymentType tipo;
    private PaymentStatus estado;
    private BigDecimal total;
    private BigDecimal totalPagado;
    private Integer numeroPlazos;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String empresaNombre;
    private String clienteNombre;
    private String facturaNumero;

    public Payment() {
    }

    public Payment(Long id, Long empresaId, Long facturaId, Long clienteId, PaymentType tipo, PaymentStatus estado, BigDecimal total, BigDecimal totalPagado, Integer numeroPlazos, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.empresaId = empresaId;
        this.facturaId = facturaId;
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.estado = estado;
        this.total = total;
        this.totalPagado = totalPagado;
        this.numeroPlazos = numeroPlazos;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Long getFacturaId() {
        return facturaId;
    }

    public void setFacturaId(Long facturaId) {
        this.facturaId = facturaId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public PaymentType getTipo() {
        return tipo;
    }

    public void setTipo(PaymentType tipo) {
        this.tipo = tipo;
    }

    public PaymentStatus getEstado() {
        return estado;
    }

    public void setEstado(PaymentStatus estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTotalPagado() {
        return totalPagado;
    }

    public void setTotalPagado(BigDecimal totalPagado) {
        this.totalPagado = totalPagado;
    }

    public Integer getNumeroPlazos() {
        return numeroPlazos;
    }

    public void setNumeroPlazos(Integer numeroPlazos) {
        this.numeroPlazos = numeroPlazos;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEmpresaNombre() {
        return empresaNombre;
    }

    public void setEmpresaNombre(String empresaNombre) {
        this.empresaNombre = empresaNombre;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getFacturaNumero() {
        return facturaNumero;
    }

    public void setFacturaNumero(String facturaNumero) {
        this.facturaNumero = facturaNumero;
    }

    public BigDecimal getTotalPendiente() {
        BigDecimal pagado = totalPagado == null ? BigDecimal.ZERO : totalPagado;
        BigDecimal totalBase = total == null ? BigDecimal.ZERO : total;
        return totalBase.subtract(pagado);
    }
}
