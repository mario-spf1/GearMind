package com.gearmind.application.product;

import java.math.BigDecimal;

public record SaveProductRequest(Long id, long empresaId, String nombre, String descripcion, String referencia, String categoria, Integer stock, Integer stockMinimo, BigDecimal precioCompra, BigDecimal precioVenta) {
}