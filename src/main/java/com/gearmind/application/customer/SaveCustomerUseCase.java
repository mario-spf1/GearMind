package com.gearmind.application.customer;

import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;

public class SaveCustomerUseCase {

    private final CustomerRepository repository;

    public SaveCustomerUseCase(CustomerRepository repository) {
        this.repository = repository;
    }

    public Customer save(SaveCustomerRequest request) {
        validate(request);

        if (request.id() == null) {
            return repository.create(request.empresaId(), request.nombre(), request.email(), request.telefono());
        }

        return repository.update(request.id(), request.empresaId(), request.nombre(), request.email(), request.telefono());
    }

    private void validate(SaveCustomerRequest r) {
        if (r.empresaId() <= 0) {
            throw new IllegalArgumentException("empresaId debe ser > 0");
        }

        if (r.nombre() == null || r.nombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
    }
}
