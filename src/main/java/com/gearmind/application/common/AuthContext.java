package com.gearmind.application.common;

import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;

/**
 * Utilidad para consultar permisos del usuario actual.
 */
public final class AuthContext {

    private AuthContext() {
    }

    private static User getUserOrThrow() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No hay usuario en sesión");
        }
        return user;
    }

    public static boolean isLoggedIn() {
        return SessionManager.getInstance().isLoggedIn();
    }

    public static boolean isAdmin() {
        User user = getUserOrThrow();
        return user.getRol() == UserRole.ADMIN;
    }

    public static boolean isEmpleado() {
        User user = getUserOrThrow();
        return user.getRol() == UserRole.EMPLEADO;
    }

    public static boolean isSuperAdmin() {
        User user = getUserOrThrow();
        return user.getRol() == UserRole.SUPER_ADMIN;
    }

    public static boolean isAdminOrSuperAdmin() {
        User user = getUserOrThrow();
        UserRole rol = user.getRol();
        return rol == UserRole.ADMIN || rol == UserRole.SUPER_ADMIN;
    }

    public static UserRole getRole() {
        return getUserOrThrow().getRol();
    }

    public static long getEmpresaId() {
        return SessionManager.getInstance().getCurrentEmpresaId();
    }

    public static String getEmpresaNombre() {
        return SessionManager.getInstance().getCurrentEmpresaNombre();
    }

    public static User getCurrentUser() {
        return getUserOrThrow();
    }
}
