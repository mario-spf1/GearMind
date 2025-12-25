package com.gearmind.domain.repair;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Repair {

    private Long id;
    private Long empresaId;
    private Long citaId;
    private Long clienteId;
    private Long vehiculoId;
    private String descripcion;
    private RepairStatus estado;
    private BigDecimal importeEstimado;
    private BigDecimal importeFinal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String empresaNombre;
    private String clienteNombre;
    private String vehiculoEtiqueta;

    public Repair() {
    }

    public Repair(Long id, Long empresaId, Long citaId, Long clienteId, Long vehiculoId, String descripcion, RepairStatus estado, BigDecimal importeEstimado, BigDecimal importeFinal, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.empresaId = empresaId;
        this.citaId = citaId;
        this.clienteId = clienteId;
        this.vehiculoId = vehiculoId;
        this.descripcion = descripcion;
        this.estado = estado;
        this.importeEstimado = importeEstimado;
        this.importeFinal = importeFinal;
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

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public RepairStatus getEstado() {
        return estado;
    }

    public void setEstado(RepairStatus estado) {
        this.estado = estado;
    }

    public BigDecimal getImporteEstimado() {
        return importeEstimado;
    }

    public void setImporteEstimado(BigDecimal importeEstimado) {
        this.importeEstimado = importeEstimado;
    }

    public BigDecimal getImporteFinal() {
        return importeFinal;
    }

    public void setImporteFinal(BigDecimal importeFinal) {
        this.importeFinal = importeFinal;
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
