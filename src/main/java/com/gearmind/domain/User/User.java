package com.gearmind.domain.user;

public class User {

    private final long id;
    private final long empresaId;
    private final String nombre;
    private final String email;
    private final String passwordHash;
    private final UserRole rol;
    private final boolean activo;

    public User(long id, long empresaId, String nombre, String email, String passwordHash, UserRole rol, boolean activo) {
        this.id = id;
        this.empresaId = empresaId;
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
    }

    public long getId() {
        return id;
    }

    public long getEmpresaId() {
        return empresaId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }
}
