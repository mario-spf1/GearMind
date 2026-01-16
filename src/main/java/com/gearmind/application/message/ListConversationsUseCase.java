package com.gearmind.application.message;

import com.gearmind.domain.message.ConversationSummary;
import com.gearmind.domain.message.MessageRepository;

import java.util.List;

public class ListConversationsUseCase {

    private final MessageRepository repository;

    public ListConversationsUseCase(MessageRepository repository) {
        this.repository = repository;
    }

    public List<ConversationSummary> execute(long empresaId, long userId) {
        return repository.findConversations(empresaId, userId);
    }
}
