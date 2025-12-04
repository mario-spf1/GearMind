package com.gearmind.domain.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(String email);

    Optional<User> findById(long id);

    List<User> findByEmpresaId(long empresaId);

    User create(long empresaId, String nombre, String email, String passwordHash, UserRole rol, boolean activo);

    User update(long id, long empresaId, String nombre, String email, String passwordHash, UserRole rol, boolean activo);

    void deactivate(long id, long empresaId);
}
