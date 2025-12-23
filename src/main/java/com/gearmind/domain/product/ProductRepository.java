package com.gearmind.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    List<Product> findAll();

    List<Product> findByEmpresaId(long empresaId);

    Optional<Product> findById(long id);

    Product create(long empresaId, String nombre, String descripcion, String referencia, String categoria, int stock, int stockMinimo, java.math.BigDecimal precioCompra, java.math.BigDecimal precioVenta);

    Product update(long id, long empresaId, String nombre, String descripcion, String referencia, String categoria, int stock, int stockMinimo, java.math.BigDecimal precioCompra, java.math.BigDecimal precioVenta);

    void deactivate(long productId, long empresaId);

    void activate(long productId, long empresaId);

    List<Product> findAllWithEmpresa();
}