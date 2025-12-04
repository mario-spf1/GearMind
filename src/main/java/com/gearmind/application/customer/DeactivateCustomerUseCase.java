package com.gearmind.application.customer;

import com.gearmind.domain.customer.CustomerRepository;

public class DeactivateCustomerUseCase {

    private final CustomerRepository repository;

    public DeactivateCustomerUseCase(CustomerRepository repository) {
        this.repository = repository;
    }

    public void deactivate(long id, long empresaId) {
        repository.deactivate(id, empresaId);
    }
}

