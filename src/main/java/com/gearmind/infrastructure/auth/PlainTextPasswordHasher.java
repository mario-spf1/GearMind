package com.gearmind.infrastructure.auth;

import com.gearmind.domain.security.PasswordHasher;

public class PlainTextPasswordHasher implements PasswordHasher {

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null) {
            return false;
        }
        return rawPassword.equals(passwordHash);
    }
}
