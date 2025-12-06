package com.gearmind.application.customer;

import com.gearmind.domain.customer.CustomerRepository;

public class ActivateCustomerUseCase {

    private final CustomerRepository repository;

    public ActivateCustomerUseCase(CustomerRepository repository) {
        this.repository = repository;
    }

    public void activate(long customerId, long empresaId) {
        repository.activate(customerId, empresaId);
    }
}
