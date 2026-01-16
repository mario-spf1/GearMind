package com.gearmind.application.message;

import com.gearmind.domain.message.InternalMessage;
import com.gearmind.domain.message.MessageRepository;

public class SendMessageUseCase {

    private final MessageRepository repository;

    public SendMessageUseCase(MessageRepository repository) {
        this.repository = repository;
    }

    public InternalMessage execute(long empresaId, long senderId, long receiverId, String contenido) {
        return repository.sendMessage(empresaId, senderId, receiverId, contenido);
    }
}
