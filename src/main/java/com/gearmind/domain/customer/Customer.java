package com.gearmind.domain.customer;

public class Customer {

    private final long id;
    private final long empresaId;
    private final String nombre;
    private final String email;
    private final String telefono;

    public Customer(long id,
                    long empresaId,
                    String nombre,
                    String email,
                    String telefono) {
        this.id = id;
        this.empresaId = empresaId;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
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
}

