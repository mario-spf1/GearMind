package com.gearmind.domain.telegram;

import java.time.LocalDateTime;

public class TelegramNotificationLog {

    private final Long id;
    private final long empresaId;
    private final long clienteId;
    private final long chatId;
    private final String tipoEvento;
    private final String payload;
    private final LocalDateTime enviadoAt;
    private final String resultado;

    public TelegramNotificationLog(Long id, long empresaId, long clienteId, long chatId, String tipoEvento, String payload, LocalDateTime enviadoAt, String resultado) {
        this.id = id;
        this.empresaId = empresaId;
        this.clienteId = clienteId;
        this.chatId = chatId;
        this.tipoEvento = tipoEvento;
        this.payload = payload;
        this.enviadoAt = enviadoAt;
        this.resultado = resultado;
    }

    public Long getId() {
        return id;
    }

    public long getEmpresaId() {
        return empresaId;
    }

    public long getClienteId() {
        return clienteId;
    }

    public long getChatId() {
        return chatId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getEnviadoAt() {
        return enviadoAt;
    }

    public String getResultado() {
        return resultado;
    }
}
