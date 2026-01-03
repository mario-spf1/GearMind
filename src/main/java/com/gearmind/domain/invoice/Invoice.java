package com.gearmind.domain.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Invoice {

    private Long id;
    private Long empresaId;
    private Long clienteId;
    private Long vehiculoId;
    private Long presupuestoId;
    private String numero;
    private LocalDateTime fecha;
    private InvoiceStatus estado;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;
    private String observaciones;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String empresaNombre;
    private String clienteNombre;
    private String vehiculoEtiqueta;

    public Invoice() {
    }

    public Invoice(Long id, Long empresaId, Long clienteId, Long vehiculoId, Long presupuestoId, String numero, LocalDateTime fecha, InvoiceStatus estado, BigDecimal subtotal, BigDecimal iva, BigDecimal total, String observaciones, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.empresaId = empresaId;
        this.clienteId = clienteId;
        this.vehiculoId = vehiculoId;
        this.presupuestoId = presupuestoId;
        this.numero = numero;
        this.fecha = fecha;
        this.estado = estado;
        this.subtotal = subtotal;
        this.iva = iva;
        this.total = total;
        this.observaciones = observaciones;
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

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(Long vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public Long getPresupuestoId() {
        return presupuestoId;
    }

    public void setPresupuestoId(Long presupuestoId) {
        this.presupuestoId = presupuestoId;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public InvoiceStatus getEstado() {
        return estado;
    }

    public void setEstado(InvoiceStatus estado) {
        this.estado = estado;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getIva() {
        return iva;
    }

    public void setIva(BigDecimal iva) {
        this.iva = iva;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
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

    public String getVehiculoEtiqueta() {
        return vehiculoEtiqueta;
    }

    public void setVehiculoEtiqueta(String vehiculoEtiqueta) {
        this.vehiculoEtiqueta = vehiculoEtiqueta;
    }
}
