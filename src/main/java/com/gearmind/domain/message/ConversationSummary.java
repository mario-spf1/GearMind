package com.gearmind.domain.message;

import java.time.LocalDateTime;

public class ConversationSummary {

    private final long contactoId;
    private final String contactoNombre;
    private final String ultimoMensaje;
    private final LocalDateTime ultimoMensajeAt;

    public ConversationSummary(long contactoId, String contactoNombre, String ultimoMensaje, LocalDateTime ultimoMensajeAt) {
        this.contactoId = contactoId;
        this.contactoNombre = contactoNombre;
        this.ultimoMensaje = ultimoMensaje;
        this.ultimoMensajeAt = ultimoMensajeAt;
    }

    public long getContactoId() {
        return contactoId;
    }

    public String getContactoNombre() {
        return contactoNombre;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public LocalDateTime getUltimoMensajeAt() {
        return ultimoMensajeAt;
    }
}
