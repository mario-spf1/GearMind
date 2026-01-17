package com.gearmind.domain.telegram;

import java.time.LocalDateTime;

public class TelegramClientLink {

    private final long id;
    private final long empresaId;
    private final long clienteId;
    private final long chatId;
    private final String username;
    private final LocalDateTime createdAt;

    public TelegramClientLink(long id, long empresaId, long clienteId, long chatId, String username, LocalDateTime createdAt) {
        this.id = id;
        this.empresaId = empresaId;
        this.clienteId = clienteId;
        this.chatId = chatId;
        this.username = username;
        this.createdAt = createdAt;
    }

    public long getId() {
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

    public String getUsername() {
        return username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
