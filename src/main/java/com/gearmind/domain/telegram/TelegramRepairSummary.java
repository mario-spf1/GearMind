package com.gearmind.domain.telegram;

public class TelegramRepairSummary {

    private final long id;
    private final String descripcion;
    private final String estado;

    public TelegramRepairSummary(long id, String descripcion, String estado) {
        this.id = id;
        this.descripcion = descripcion;
        this.estado = estado;
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
}
