package com.gearmind.application.common;

import com.gearmind.domain.user.User;

/**
 * Gestiona la sesión actual de la aplicación.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();
    private User currentUser;
    private String currentEmpresaNombre;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public synchronized void startSession(User user) {
        startSession(user, null);
    }

    public synchronized void startSession(User user, String empresaNombre) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser null");
        }
        this.currentUser = user;
        this.currentEmpresaNombre = empresaNombre;
    }

    /** Cierra la sesión actual. */
    public synchronized void clearSession() {
        this.currentUser = null;
        this.currentEmpresaNombre = null;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public synchronized long getCurrentEmpresaId() {
        if (currentUser == null) {
            throw new IllegalStateException("No hay usuario en sesión");
        }
        return currentUser.getEmpresaId();
    }

    public synchronized String getCurrentEmpresaNombre() {
        return currentEmpresaNombre;
    }
}
