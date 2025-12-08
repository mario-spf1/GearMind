package com.gearmind.application.user;

import com.gearmind.domain.user.UserRole;

public record SaveUserRequest(Long id, long empresaId, String nombre, String email, String rawPassword, UserRole rol, boolean activo) {
}
