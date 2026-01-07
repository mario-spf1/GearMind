package com.gearmind.application.user;

import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.user.UserRepository;

public class DeleteUserUseCase {

    private final UserRepository repository;

    public DeleteUserUseCase(UserRepository repository) {
        this.repository = repository;
    }

    public void delete(long id, long empresaId) {
        if (!AuthContext.isAdminOrSuperAdmin()) {
            throw new IllegalStateException("No tienes permisos para eliminar usuarios.");
        }
        repository.delete(id, empresaId);
    }
}
