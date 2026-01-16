package com.gearmind.domain.message;

import java.time.LocalDateTime;

public class InternalMessage {

    private Long id;
    private final long empresaId;
    private final long senderId;
    private final long receiverId;
    private final String contenido;
    private final LocalDateTime createdAt;
    private final LocalDateTime readAt;

    public InternalMessage(Long id, long empresaId, long senderId, long receiverId, String contenido, LocalDateTime createdAt, LocalDateTime readAt) {
        this.id = id;
        this.empresaId = empresaId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.contenido = contenido;
        this.createdAt = createdAt;
        this.readAt = readAt;
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

    public long getSenderId() {
        return senderId;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public String getContenido() {
        return contenido;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }
}
