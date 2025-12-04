package com.gearmind.domain.customer;

public class Customer {

    private final long id;
    private final long empresaId;
    private final String nombre;
    private final String email;
    private final String telefono;
    private final String notas;
    private final boolean activo;

    public Customer(long id, long empresaId, String nombre, String email, String telefono, String notas, boolean activo) {
        this.id = id;
        this.empresaId = empresaId;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.notas = notas;
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

    public String getTelefono() {
        return telefono;
    }

    public String getNotas() {
        return notas;
    }

    public boolean isActivo() {
        return activo;
    }
}
