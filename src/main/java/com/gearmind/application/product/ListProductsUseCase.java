package com.gearmind.application.product;

import com.gearmind.domain.product.Product;
import com.gearmind.domain.product.ProductRepository;

import java.util.List;

public class ListProductsUseCase {

    private final ProductRepository repository;

    public ListProductsUseCase(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> listByEmpresa(long empresaId) {
        if (empresaId <= 0) {
            throw new IllegalArgumentException("empresaId debe ser > 0");
        }
        return repository.findByEmpresaId(empresaId);
    }

    public List<Product> listAllWithEmpresa() {
        return repository.findAllWithEmpresa();
    }
}