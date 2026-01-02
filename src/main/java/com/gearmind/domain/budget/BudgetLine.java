package com.gearmind.domain.budget;

import java.math.BigDecimal;

public class BudgetLine {

    private Long id;
    private Long presupuestoId;
    private Long productoId;
    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal precio;
    private BigDecimal total;

    public BudgetLine() {
    }

    public BudgetLine(Long id, Long presupuestoId, Long productoId, String descripcion, BigDecimal cantidad, BigDecimal precio, BigDecimal total) {
        this.id = id;
        this.presupuestoId = presupuestoId;
        this.productoId = productoId;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precio = precio;
        this.total = total;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPresupuestoId() {
        return presupuestoId;
    }

    public void setPresupuestoId(Long presupuestoId) {
        this.presupuestoId = presupuestoId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
