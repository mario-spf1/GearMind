package com.gearmind.domain.company;

public class Empresa {

    private final long id;
    private final String nombre;
    private final String cif;
    private final String telefono;
    private final String email;
    private final String direccion;
    private final String ciudad;
    private final String provincia;
    private final String cp;
    private final boolean activa;

    public Empresa(long id,
                   String nombre,
                   String cif,
                   String telefono,
                   String email,
                   String direccion,
                   String ciudad,
                   String provincia,
                   String cp,
                   boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.cif = cif;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.provincia = provincia;
        this.cp = cp;
        this.activa = activa;
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCif() {
        return cif;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getProvincia() {
        return provincia;
    }

    public String getCp() {
        return cp;
    }

    public boolean isActiva() {
        return activa;
    }

    public boolean isNew() {
        return id == 0;
    }
}
