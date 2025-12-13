package com.gearmind.application.customer;

import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;

import java.util.List;

public class ListCustomersUseCase {

    private final CustomerRepository repository;

    public ListCustomersUseCase(CustomerRepository repository) {
        this.repository = repository;
    }

    public List<Customer> listByEmpresa(long empresaId) {
        if (empresaId <= 0) {
            throw new IllegalArgumentException("empresaId debe ser > 0");
        }
        return repository.findByEmpresaId(empresaId);
    }

    public Object listVisibleForCurrentUser() {
        if (AuthContext.isSuperAdmin()) {
            return repository.findAllWithEmpresa();
        }
        return repository.findByEmpresaId(AuthContext.getEmpresaId());
    }
    
    public List<Customer> listAllWithEmpresa() {
        return repository.findAllWithEmpresa();
    }

}
