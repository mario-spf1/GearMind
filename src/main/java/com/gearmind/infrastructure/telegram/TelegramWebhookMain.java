package com.gearmind.infrastructure.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gearmind.application.telegram.TelegramMessageHandlerUseCase;
import java.util.concurrent.CountDownLatch;

public class TelegramWebhookMain {

    public static void main(String[] args) throws Exception {
        TelegramConfig config = new TelegramConfig();
        ObjectMapper objectMapper = new ObjectMapper();
        TelegramBotClient botClient = new TelegramBotClient(config, objectMapper);
        MySqlTelegramRepository repository = new MySqlTelegramRepository();
        TelegramMessageHandlerUseCase handlerUseCase = new TelegramMessageHandlerUseCase( config, botClient, repository, repository, repository, repository, objectMapper);
        TelegramWebhookServer server = new TelegramWebhookServer(config, handlerUseCase, objectMapper);
        server.start();

        new CountDownLatch(1).await();
    }
}
