package com.gearmind.application.product;

import com.gearmind.domain.product.Product;
import com.gearmind.domain.product.ProductRepository;

import java.math.BigDecimal;

public class SaveProductUseCase {

    private final ProductRepository repository;

    public SaveProductUseCase(ProductRepository repository) {
        this.repository = repository;
    }

    public Product save(SaveProductRequest request) {
        validate(request);

        int stock = defaultInt(request.stock());
        int stockMinimo = defaultInt(request.stockMinimo());
        BigDecimal precioCompra = defaultDecimal(request.precioCompra());
        BigDecimal precioVenta = defaultDecimal(request.precioVenta());

        if (request.id() == null) {
            return repository.create(request.empresaId(), request.nombre().trim(), normalize(request.descripcion()), normalize(request.referencia()), normalize(request.categoria()), stock, stockMinimo, precioCompra, precioVenta);
        }

        return repository.update(request.id(), request.empresaId(), request.nombre().trim(), normalize(request.descripcion()), normalize(request.referencia()), normalize(request.categoria()), stock, stockMinimo, precioCompra, precioVenta);
    }

    private String normalize(String s) {
        return s == null ? null : s.trim();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validate(SaveProductRequest r) {
        if (r.empresaId() <= 0) {
            throw new IllegalArgumentException("empresaId debe ser > 0");
        }

        if (r.nombre() == null || r.nombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }

        if (r.nombre().trim().length() > 150) {
            throw new IllegalArgumentException("El nombre no puede superar 150 caracteres.");
        }

        if (r.referencia() != null && r.referencia().trim().length() > 100) {
            throw new IllegalArgumentException("La referencia no puede superar 100 caracteres.");
        }

        if (r.categoria() != null && r.categoria().trim().length() > 100) {
            throw new IllegalArgumentException("La categoría no puede superar 100 caracteres.");
        }

        if (r.stock() != null && r.stock() < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }

        if (r.stockMinimo() != null && r.stockMinimo() < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");
        }

        if (r.precioCompra() != null && r.precioCompra().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio de compra no puede ser negativo.");
        }

        if (r.precioVenta() != null && r.precioVenta().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio de venta no puede ser negativo.");
        }
    }
}
