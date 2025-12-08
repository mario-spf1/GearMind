package com.gearmind.domain.company;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository {

    List<Empresa> findAll();

    Optional<Empresa> findById(long id);

    /**
     * Inserta o actualiza una empresa.
     * Si empresa.isNew() => inserta y devuelve con id generado.
     * Si no => actualiza.
     * @param empresa
     * @return 
     */
    Empresa save(Empresa empresa);

    void deleteById(long id);
}
