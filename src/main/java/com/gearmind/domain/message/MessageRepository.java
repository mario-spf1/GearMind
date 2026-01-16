package com.gearmind.domain.message;

import java.util.List;

public interface MessageRepository {

    List<ConversationSummary> findConversations(long empresaId, long userId);

    List<InternalMessage> findMessages(long empresaId, long userId, long otherUserId);

    InternalMessage sendMessage(long empresaId, long senderId, long receiverId, String contenido);
}
