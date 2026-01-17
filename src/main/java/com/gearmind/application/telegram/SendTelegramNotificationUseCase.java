package com.gearmind.application.telegram;

import com.gearmind.domain.telegram.TelegramClientLink;
import com.gearmind.domain.telegram.TelegramClientLinkRepository;
import com.gearmind.domain.telegram.TelegramNotificationLog;
import com.gearmind.domain.telegram.TelegramNotificationLogRepository;
import com.gearmind.infrastructure.telegram.TelegramBotClient;
import com.gearmind.infrastructure.telegram.TelegramConfig;

import java.time.LocalDateTime;
import java.util.Optional;

public class SendTelegramNotificationUseCase {

    private final TelegramConfig config;
    private final TelegramBotClient botClient;
    private final TelegramClientLinkRepository clientLinkRepository;
    private final TelegramNotificationLogRepository notificationLogRepository;

    public SendTelegramNotificationUseCase(TelegramConfig config,
            TelegramBotClient botClient,
            TelegramClientLinkRepository clientLinkRepository,
            TelegramNotificationLogRepository notificationLogRepository) {
        this.config = config;
        this.botClient = botClient;
        this.clientLinkRepository = clientLinkRepository;
        this.notificationLogRepository = notificationLogRepository;
    }

    public void execute(long clienteId, String tipoEvento, String mensaje, String payload) {
        Optional<TelegramClientLink> link = clientLinkRepository.findByClienteId(config.getEmpresaId(), clienteId);
        if (link.isEmpty()) {
            notificationLogRepository.save(new TelegramNotificationLog(null, config.getEmpresaId(),clienteId, 0L, tipoEvento, payload, LocalDateTime.now(), "SIN_VINCULO"));
            return;
        }

        boolean ok = botClient.sendMessage(link.get().getChatId(), mensaje);
        notificationLogRepository.save(new TelegramNotificationLog(null, config.getEmpresaId(), clienteId, link.get().getChatId(), tipoEvento, payload, LocalDateTime.now(), ok ? "ENVIADO" : "ERROR"));
    }
}
