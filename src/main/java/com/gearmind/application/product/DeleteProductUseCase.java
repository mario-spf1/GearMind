package com.gearmind.application.product;

import com.gearmind.domain.product.ProductRepository;

public class DeleteProductUseCase {

    private final ProductRepository repository;

    public DeleteProductUseCase(ProductRepository repository) {
        this.repository = repository;
    }

    public void delete(long productId, long empresaId) {
        if (productId <= 0) {
            throw new IllegalArgumentException("El producto es obligatorio.");
        }
        if (empresaId <= 0) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        repository.delete(productId, empresaId);
    }
}
