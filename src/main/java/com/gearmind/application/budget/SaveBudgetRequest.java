package com.gearmind.application.budget;

import com.gearmind.domain.budget.BudgetLine;
import com.gearmind.domain.budget.BudgetStatus;
import java.util.List;

public class SaveBudgetRequest {

    private Long id;
    private Long empresaId;
    private Long clienteId;
    private Long vehiculoId;
    private Long reparacionId;
    private BudgetStatus estado;
    private String observaciones;
    private List<BudgetLine> lineas;

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

    public List<BudgetLine> getLineas() {
        return lineas;
    }

    public void setLineas(List<BudgetLine> lineas) {
        this.lineas = lineas;
    }
}
