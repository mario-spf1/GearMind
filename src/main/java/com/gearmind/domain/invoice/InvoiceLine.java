package com.gearmind.domain.invoice;

import java.math.BigDecimal;

public class InvoiceLine {

    private Long id;
    private Long facturaId;
    private Long productoId;
    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal precio;
    private BigDecimal total;

    public InvoiceLine() {
    }

    public InvoiceLine(Long id, Long facturaId, Long productoId, String descripcion, BigDecimal cantidad, BigDecimal precio, BigDecimal total) {
        this.id = id;
        this.facturaId = facturaId;
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

    public Long getFacturaId() {
        return facturaId;
    }

    public void setFacturaId(Long facturaId) {
        this.facturaId = facturaId;
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
