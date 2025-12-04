package com.gearmind.application.user;

import com.gearmind.domain.user.UserRepository;

public class DeactivateUserUseCase {

    private final UserRepository repository;

    public DeactivateUserUseCase(UserRepository repository) {
        this.repository = repository;
    }

    public void deactivate(long id, long empresaId) {
        repository.deactivate(id, empresaId);
    }
}
