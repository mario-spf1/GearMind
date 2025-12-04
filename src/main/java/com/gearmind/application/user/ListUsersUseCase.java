package com.gearmind.application.user;

import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRepository;

import java.util.List;

public class ListUsersUseCase {

    private final UserRepository repository;

    public ListUsersUseCase(UserRepository repository) {
        this.repository = repository;
    }

    public List<User> listByEmpresa(long empresaId) {
        if (empresaId <= 0) {
            throw new IllegalArgumentException("empresaId debe ser > 0");
        }
        return repository.findByEmpresaId(empresaId);
    }
}
