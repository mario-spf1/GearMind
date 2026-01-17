package com.gearmind.domain.telegram;

import java.util.Optional;

public interface TelegramClientLinkRepository {

    Optional<TelegramClientLink> findByChatId(long empresaId, long chatId);

    Optional<TelegramClientLink> findByClienteId(long empresaId, long clienteId);
}
