package com.gearmind.application.product;

import com.gearmind.domain.product.ProductRepository;

public class DeactivateProductUseCase {

    private final ProductRepository repository;

    public DeactivateProductUseCase(ProductRepository repository) {
        this.repository = repository;
    }

    public void deactivate(long productId, long empresaId) {
        repository.deactivate(productId, empresaId);
    }
}
