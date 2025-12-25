package com.gearmind.application.repair;

import com.gearmind.domain.repair.RepairStatus;

import java.math.BigDecimal;

public class SaveRepairRequest {

    private Long id;
    private Long empresaId;
    private Long citaId;
    private Long clienteId;
    private Long vehiculoId;
    private String descripcion;
    private RepairStatus estado;
    private BigDecimal importeEstimado;
    private BigDecimal importeFinal;

    public SaveRepairRequest() {
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
}
