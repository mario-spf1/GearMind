package com.gearmind.application.invoice;

import com.gearmind.domain.invoice.InvoiceLine;
import com.gearmind.domain.invoice.InvoiceStatus;
import java.util.List;

public class SaveInvoiceRequest {

    private Long id;
    private Long empresaId;
    private Long clienteId;
    private Long vehiculoId;
    private Long presupuestoId;
    private String numero;
    private InvoiceStatus estado;
    private String observaciones;
    private List<InvoiceLine> lineas;
    private Integer ivaPercent;

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

    public InvoiceStatus getEstado() {
        return estado;
    }

    public void setEstado(InvoiceStatus estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<InvoiceLine> getLineas() {
        return lineas;
    }

    public void setLineas(List<InvoiceLine> lineas) {
        this.lineas = lineas;
    }

    public Integer getIvaPercent() {
        return ivaPercent;
    }

    public void setIvaPercent(Integer ivaPercent) {
        this.ivaPercent = ivaPercent;
    }
}
