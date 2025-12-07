package com.gearmind.application.company;

import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.company.EmpresaRepository;

public class SaveEmpresaUseCase {

    private final EmpresaRepository repository;

    public SaveEmpresaUseCase(EmpresaRepository repository) {
        this.repository = repository;
    }

    public Empresa execute(SaveEmpresaRequest request) {
        String nombre = safeTrim(request.nombre());
        String cif = safeTrim(request.cif());
        String telefono = safeTrim(request.telefono());
        String email = safeTrim(request.email());
        String direccion = safeTrim(request.direccion());
        String ciudad = safeTrim(request.ciudad());
        String provincia = safeTrim(request.provincia());
        String cp = safeTrim(request.cp());
        boolean activa = request.activa() == null || request.activa();

        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre de la empresa es obligatorio");
        }

        long id = request.id() != null ? request.id() : 0L;

        Empresa empresa = new Empresa(
                id, nombre, cif, telefono, email,
                direccion, ciudad, provincia, cp, activa
        );

        return repository.save(empresa);
    }

    private String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }
}
