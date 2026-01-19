package com.gearmind.domain.telegram;

public class TelegramRepairSummary {

    private final long id;
    private final String descripcion;
    private final String estado;
    private final String vehiculo;

    public TelegramRepairSummary(long id, String descripcion, String estado, String vehiculo) {
        this.id = id;
        this.descripcion = descripcion;
        this.estado = estado;
        this.vehiculo = vehiculo;
    }

    public long getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public String getVehiculo() {
        return vehiculo;
    }
}
