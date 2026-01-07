package com.gearmind.application.customer;

import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.customer.CustomerRepository;

public class DeleteCustomerUseCase {

    private final CustomerRepository repository;

    public DeleteCustomerUseCase(CustomerRepository repository) {
        this.repository = repository;
    }

    public void delete(long customerId, long empresaId) {
        if (!AuthContext.isAdminOrSuperAdmin()) {
            throw new IllegalStateException("No tienes permisos para eliminar clientes.");
        }
        repository.delete(customerId, empresaId);
    }
}
