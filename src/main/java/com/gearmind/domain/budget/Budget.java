package com.gearmind.domain.budget;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Budget {

    private Long id;
    private Long empresaId;
    private Long clienteId;
    private Long vehiculoId;
    private Long reparacionId;
    private LocalDateTime fecha;
    private BudgetStatus estado;
    private String observaciones;
    private BigDecimal totalEstimado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String empresaNombre;
    private String clienteNombre;
    private String vehiculoEtiqueta;
    private String reparacionDescripcion;

    public Budget() {
    }

    public Budget(Long id, Long empresaId, Long clienteId, Long vehiculoId, Long reparacionId, LocalDateTime fecha, BudgetStatus estado, String observaciones, BigDecimal totalEstimado, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.empresaId = empresaId;
        this.clienteId = clienteId;
        this.vehiculoId = vehiculoId;
        this.reparacionId = reparacionId;
        this.fecha = fecha;
        this.estado = estado;
        this.observaciones = observaciones;
        this.totalEstimado = totalEstimado;
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

    public Long getReparacionId() {
        return reparacionId;
    }

    public void setReparacionId(Long reparacionId) {
        this.reparacionId = reparacionId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BudgetStatus getEstado() {
        return estado;
    }

    public void setEstado(BudgetStatus estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public BigDecimal getTotalEstimado() {
        return totalEstimado;
    }

    public void setTotalEstimado(BigDecimal totalEstimado) {
        this.totalEstimado = totalEstimado;
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

    public String getReparacionDescripcion() {
        return reparacionDescripcion;
    }

    public void setReparacionDescripcion(String reparacionDescripcion) {
        this.reparacionDescripcion = reparacionDescripcion;
    }
}
