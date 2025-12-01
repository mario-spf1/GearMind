package com.gearmind.domain.customer;

import java.util.List;

public interface CustomerRepository {

    /**
     * Devuelve todos los clientes de una empresa.
     */
    List<Customer> findByEmpresaId(long empresaId);
}

