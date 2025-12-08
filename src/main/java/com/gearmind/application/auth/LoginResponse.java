package com.gearmind.application.auth;

import com.gearmind.domain.user.User;

public record LoginResponse(User user, Long empresaId, String empresaNombre) {
}
