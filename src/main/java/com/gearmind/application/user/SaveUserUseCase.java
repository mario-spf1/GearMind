package com.gearmind.application.user;

import com.gearmind.domain.security.PasswordHasher;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRepository;

import java.util.regex.Pattern;

public class SaveUserUseCase {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;

    public SaveUserUseCase(UserRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    public User save(SaveUserRequest request) {
        validate(request);

        if (request.id() == null) {
            String passwordHash = passwordHasher.hash(request.rawPassword().trim());

            return repository.create(
                    request.empresaId(),
                    request.nombre().trim(),
                    normalize(request.email()),
                    passwordHash,
                    request.rol(),
                    request.activo()
            );
        }

        User existing = repository.findById(request.id()).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String passwordHash;
        if (request.rawPassword() != null && !request.rawPassword().isBlank()) {
            passwordHash = passwordHasher.hash(request.rawPassword().trim());
        } else {
            passwordHash = existing.getPasswordHash();
        }

        return repository.update(
                request.id(),
                request.empresaId(),
                request.nombre().trim(),
                normalize(request.email()),
                passwordHash,
                request.rol(),
                request.activo()
        );
    }

    private void validate(SaveUserRequest r) {
        if (r.empresaId() <= 0) {
            throw new IllegalArgumentException("empresaId debe ser > 0");
        }

        if (r.nombre() == null || r.nombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (r.nombre().trim().length() > 100) {
            throw new IllegalArgumentException("El nombre no puede superar 100 caracteres.");
        }

        if (r.email() == null || r.email().trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }
        String email = r.email().trim();
        if (email.length() > 150) {
            throw new IllegalArgumentException("El email no puede superar 150 caracteres.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("El email no tiene un formato válido.");
        }

        if (r.id() == null) {
            if (r.rawPassword() == null || r.rawPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("La contraseña es obligatoria para nuevos usuarios.");
            }
            if (r.rawPassword().trim().length() < 8) {
                throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
            }
        }
    }

    private String normalize(String s) {
        return s == null ? null : s.trim();
    }
}
