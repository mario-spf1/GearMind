package com.gearmind.infrastructure.auth;

import com.gearmind.domain.security.PasswordHasher;
import org.mindrot.jbcrypt.BCrypt;

public class BCryptPasswordHasher implements PasswordHasher {

    private static final int COST = 12;

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Método de ayuda para generar hashes cuando crees usuarios.
     * @param rawPassword
     * @return 
     */
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(COST));
    }
}

