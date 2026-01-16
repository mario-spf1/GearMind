package com.gearmind.application.message;

import com.gearmind.domain.message.InternalMessage;
import com.gearmind.domain.message.MessageRepository;

import java.util.List;

public class ListMessagesUseCase {

    private final MessageRepository repository;

    public ListMessagesUseCase(MessageRepository repository) {
        this.repository = repository;
    }

    public List<InternalMessage> execute(long empresaId, long userId, long otherUserId) {
        return repository.findMessages(empresaId, userId, otherUserId);
    }
}
 