package com.gearmind.domain.telegram;

import java.time.LocalDateTime;

public class TelegramAppointmentRequest {

    private Long id;
    private final long empresaId;
    private final Long clienteId;
    private final Long vehiculoId;
    private final long chatId;
    private final String mensaje;
    private final LocalDateTime fecha;
    private final TelegramAppointmentRequestStatus estado;

    public TelegramAppointmentRequest(Long id, long empresaId, Long clienteId, Long vehiculoId, long chatId, String mensaje, LocalDateTime fecha, TelegramAppointmentRequestStatus estado) {
        this.id = id;
        this.empresaId = empresaId;
        this.clienteId = clienteId;
        this.vehiculoId = vehiculoId;
        this.chatId = chatId;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getEmpresaId() {
        return empresaId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getVehiculoId() {
        return vehiculoId;
    }

    public long getChatId() {
        return chatId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public TelegramAppointmentRequestStatus getEstado() {
        return estado;
    }
}
