package com.gearmind.application.product;

import com.gearmind.domain.product.ProductRepository;

public class ActivateProductUseCase {

    private final ProductRepository repository;

    public ActivateProductUseCase(ProductRepository repository) {
        this.repository = repository;
    }

    public void activate(long productId, long empresaId) {
        repository.activate(productId, empresaId);
    }
}
