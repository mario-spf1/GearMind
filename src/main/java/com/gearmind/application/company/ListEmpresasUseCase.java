package com.gearmind.application.company;

import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.company.EmpresaRepository;

import java.util.List;

public class ListEmpresasUseCase {

    private final EmpresaRepository repository;

    public ListEmpresasUseCase(EmpresaRepository repository) {
        this.repository = repository;
    }

    public List<Empresa> execute() {
        return repository.findAll();
    }
}
