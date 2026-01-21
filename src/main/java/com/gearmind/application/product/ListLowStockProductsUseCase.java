package com.gearmind.application.product;

import com.gearmind.domain.product.Product;
import com.gearmind.domain.product.ProductRepository;

import java.util.List;

public class ListLowStockProductsUseCase {

    private final ProductRepository repository;

    public ListLowStockProductsUseCase(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> listByEmpresa(long empresaId) {
        if (empresaId <= 0) {
            throw new IllegalArgumentException("empresaId debe ser > 0");
        }
        return repository.findLowStockByEmpresa(empresaId);
    }

    public List<Product> listAllWithEmpresa() {
        return repository.findLowStockAllWithEmpresa();
    }
}
