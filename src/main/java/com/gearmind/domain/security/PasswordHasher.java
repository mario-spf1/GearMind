package com.gearmind.domain.security;

public interface PasswordHasher {
    boolean matches(String rawPassword, String passwordHash);
}
