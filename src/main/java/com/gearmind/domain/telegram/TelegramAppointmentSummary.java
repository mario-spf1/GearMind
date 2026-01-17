package com.gearmind.domain.telegram;

import java.time.LocalDateTime;

public class TelegramAppointmentSummary {

    private final long id;
    private final LocalDateTime fechaHora;
    private final String estado;
    private final String notas;

    public TelegramAppointmentSummary(long id, LocalDateTime fechaHora, String estado, String notas) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.estado = estado;
        this.notas = notas;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getEstado() {
        return estado;
    }

    public String getNotas() {
        return notas;
    }
}
