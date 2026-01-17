package com.gearmind.infrastructure.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gearmind.application.telegram.TelegramMessageHandlerUseCase;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import java.util.concurrent.CountDownLatch;

public class TelegramWebhookMain {

    public static void main(String[] args) throws Exception {
        TelegramConfig config = new TelegramConfig();
        ObjectMapper objectMapper = new ObjectMapper();
        TelegramBotClient botClient = new TelegramBotClient(config, objectMapper);
        MySqlTelegramRepository repository = new MySqlTelegramRepository();
        MySqlCustomerRepository customerRepository = new MySqlCustomerRepository();
        TelegramMessageHandlerUseCase handlerUseCase = new TelegramMessageHandlerUseCase(config, botClient, repository, repository, repository, repository, customerRepository, objectMapper);
        TelegramWebhookServer server = new TelegramWebhookServer(config, handlerUseCase, objectMapper);
        server.start();

        new CountDownLatch(1).await();
    }
}
