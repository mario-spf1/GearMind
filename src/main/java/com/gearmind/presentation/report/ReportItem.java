package com.gearmind.presentation.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReportItem {

    private final String empresa;
    private final String tipo;
    private final String descripcion;
    private final String estado;
    private final LocalDate fecha;
    private final String fechaLabel;
    private final String cantidadLabel;
    private final BigDecimal total;

    public ReportItem(String empresa, String tipo, String descripcion, String estado, LocalDate fecha, String fechaLabel, String cantidadLabel, BigDecimal total) {
        this.empresa = empresa;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.estado = estado;
        this.fecha = fecha;
        this.fechaLabel = fechaLabel;
        this.cantidadLabel = cantidadLabel;
        this.total = total;
    }

    public String getEmpresa() {
        return empresa;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getFechaLabel() {
        return fechaLabel;
    }

    public String getCantidadLabel() {
        return cantidadLabel;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
