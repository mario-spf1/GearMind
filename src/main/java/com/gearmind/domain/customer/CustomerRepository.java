package com.gearmind.domain.customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {

    /**
     * Devuelve todos los clientes de una empresa.
     * @param empresaId
     * @return 
     */
    List<Customer> findByEmpresaId(long empresaId);

    Optional<Customer> findById(long id);

    Customer create(long empresaId, String nombre, String email, String telefono, String notas);

    Customer update(long id, long empresaId, String nombre, String email, String telefono, String notas);

    void deactivate(long customerId, long empresaId);

    void activate(long customerId, long empresaId);
}
