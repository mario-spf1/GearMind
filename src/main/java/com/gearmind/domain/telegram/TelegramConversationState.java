package com.gearmind.domain.telegram;

import java.time.LocalDateTime;

public class TelegramConversationState {

    private Long id;
    private final long empresaId;
    private final long chatId;
    private final TelegramConversationStep step;
    private final String payload;
    private final LocalDateTime updatedAt;

    public TelegramConversationState(Long id, long empresaId, long chatId, TelegramConversationStep step, String payload, LocalDateTime updatedAt) {
        this.id = id;
        this.empresaId = empresaId;
        this.chatId = chatId;
        this.step = step;
        this.payload = payload;
        this.updatedAt = updatedAt;
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

    public long getChatId() {
        return chatId;
    }

    public TelegramConversationStep getStep() {
        return step;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
