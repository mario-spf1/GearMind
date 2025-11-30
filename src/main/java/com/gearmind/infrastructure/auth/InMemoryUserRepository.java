package com.gearmind.infrastructure.auth;

import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRepository;
import com.gearmind.domain.user.UserRole;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> usersByEmail = new HashMap<>();

    public InMemoryUserRepository() {
        // Usuario de prueba:
        // email: admin@gearmind.local
        // password: admin
        User admin = new User(
                1L,
                1L,
                "Administrador",
                "admin@gearmind.local",
                "admin",              // de momento sin hash real
                UserRole.ADMIN,
                true
        );

        usersByEmail.put(admin.getEmail().toLowerCase(), admin);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return Optional.ofNullable(usersByEmail.get(email.toLowerCase()));
    }
}
