package com.gearmind.domain.inventory;

import java.time.LocalDateTime;

public class InventoryMovement {

    private Long id;
    private Long empresaId;
    private Long productoId;
    private InventoryMovementType tipo;
    private Integer cantidad;
    private String referencia;
    private String notas;
    private LocalDateTime createdAt;
    private String empresaNombre;
    private String productoNombre;
    private String productoReferencia;

    public InventoryMovement() {
    }

    public InventoryMovement(Long id, Long empresaId, Long productoId, InventoryMovementType tipo, Integer cantidad, String referencia, String notas, LocalDateTime createdAt) {
        this.id = id;
        this.empresaId = empresaId;
        this.productoId = productoId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.referencia = referencia;
        this.notas = notas;
        this.createdAt = createdAt;
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

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public InventoryMovementType getTipo() {
        return tipo;
    }

    public void setTipo(InventoryMovementType tipo) {
        this.tipo = tipo;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmpresaNombre() {
        return empresaNombre;
    }

    public void setEmpresaNombre(String empresaNombre) {
        this.empresaNombre = empresaNombre;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public void setProductoNombre(String productoNombre) {
        this.productoNombre = productoNombre;
    }

    public String getProductoReferencia() {
        return productoReferencia;
    }

    public void setProductoReferencia(String productoReferencia) {
        this.productoReferencia = productoReferencia;
    }
}
