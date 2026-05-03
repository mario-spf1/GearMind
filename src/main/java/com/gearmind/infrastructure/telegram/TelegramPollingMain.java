package com.gearmind.infrastructure.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gearmind.application.telegram.TelegramMessageHandlerUseCase;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import com.gearmind.infrastructure.telegram.dto.TelegramUpdate;

import java.util.List;

/**
 * Punto de entrada del bot en modo long polling.
 *
 * No necesita IP pública, ngrok ni puerto abierto: el bot llama a getUpdates
 * contra api.telegram.org y bloquea hasta que hay mensajes nuevos. Funciona
 * detrás de cualquier NAT doméstica, que es lo que tienen los talleres.
 */
public class TelegramPollingMain {

    private static final int LONG_POLL_TIMEOUT_SECONDS = 30;
    private static final long ERROR_BACKOFF_MIN_MS = 1_000L;
    private static final long ERROR_BACKOFF_MAX_MS = 30_000L;

    private static volatile boolean running = true;

    public static void main(String[] args) {
        TelegramConfig config = new TelegramConfig();
        if (config.getBotToken() == null || config.getBotToken().isBlank()) {
            System.err.println("[telegram] TELEGRAM_BOT_TOKEN no configurado. Saliendo.");
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        TelegramBotClient botClient = new TelegramBotClient(config, objectMapper);
        MySqlTelegramRepository repository = new MySqlTelegramRepository();
        MySqlCustomerRepository customerRepository = new MySqlCustomerRepository();
        TelegramMessageHandlerUseCase handlerUseCase = new TelegramMessageHandlerUseCase(
                config, botClient, repository, repository, repository, repository, customerRepository, objectMapper);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            System.out.println("[telegram] Parando bot...");
        }, "telegram-shutdown"));

        // Si una versión anterior dejó un webhook configurado, getUpdates devolvería 409.
        botClient.deleteWebhook();

        System.out.println("[telegram] Bot iniciado en modo long polling.");
        runPollingLoop(botClient, handlerUseCase);
        System.out.println("[telegram] Bot detenido.");
    }

    private static void runPollingLoop(TelegramBotClient botClient, TelegramMessageHandlerUseCase handlerUseCase) {
        long offset = 0L;
        long backoffMs = ERROR_BACKOFF_MIN_MS;

        while (running) {
            try {
                List<TelegramUpdate> updates = botClient.getUpdates(offset, LONG_POLL_TIMEOUT_SECONDS);
                backoffMs = ERROR_BACKOFF_MIN_MS;
                for (TelegramUpdate update : updates) {
                    if (!running) {
                        break;
                    }
                    long updateId = update.getUpdate_id();
                    if (updateId >= offset) {
                        offset = updateId + 1;
                    }
                    try {
                        handlerUseCase.handle(update);
                    } catch (RuntimeException e) {
                        // Un mensaje malformado no debe matar el bucle: lo logueamos y seguimos.
                        // Como ya hemos avanzado el offset, no volverá a entregarse.
                        System.err.println("[telegram] Error procesando update " + updateId + ": " + e.getMessage());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                if (!running) {
                    return;
                }
                System.err.println("[telegram] Error en getUpdates: " + e.getMessage() + " — reintento en " + backoffMs + " ms");
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                backoffMs = Math.min(backoffMs * 2, ERROR_BACKOFF_MAX_MS);
            }
        }
    }
}
