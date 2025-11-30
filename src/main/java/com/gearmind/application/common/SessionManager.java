package com.gearmind.application.common;

import com.gearmind.domain.user.User;

/**
 * Gestiona la sesión actual de la aplicación.
 * En app de escritorio solo hay una sesión a la vez.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private User currentUser;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /** Inicia sesión con un usuario.
     * @param user */
    public synchronized void startSession(User user) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser null");
        }
        this.currentUser = user;
    }

    /** Cierra la sesión actual. */
    public synchronized void clearSession() {
        this.currentUser = null;
    }

    /** Devuelve el usuario actual, o null si no hay sesión.
     * @return  */
    public synchronized User getCurrentUser() {
        return currentUser;
    }

    /** Indica si hay un usuario autenticado.
     * @return  */
    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Devuelve el id de empresa del usuario actual.
     * @return  */
    public synchronized long getCurrentEmpresaId() {
        if (currentUser == null) {
            throw new IllegalStateException("No hay usuario en sesión");
        }
        return currentUser.getEmpresaId();
    }
}
