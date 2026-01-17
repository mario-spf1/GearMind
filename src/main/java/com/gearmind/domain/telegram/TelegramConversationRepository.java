package com.gearmind.domain.telegram;

import java.util.Optional;

public interface TelegramConversationRepository {

    Optional<TelegramConversationState> findConversationByChatId(long empresaId, long chatId);

    TelegramConversationState save(TelegramConversationState state);

    void delete(long id);
}
