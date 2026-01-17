package com.gearmind.infrastructure.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gearmind.application.telegram.TelegramMessageHandlerUseCase;
import com.gearmind.infrastructure.telegram.dto.TelegramUpdate;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;

public class TelegramWebhookServer {

    private final TelegramConfig config;
    private final TelegramMessageHandlerUseCase handlerUseCase;
    private final ObjectMapper objectMapper;
    private HttpServer server;

    public TelegramWebhookServer(TelegramConfig config, TelegramMessageHandlerUseCase handlerUseCase, ObjectMapper objectMapper) {
        this.config = config;
        this.handlerUseCase = handlerUseCase;
        this.objectMapper = objectMapper;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(config.getWebhookPort()), 0);
        server.createContext(config.getWebhookPath(), new WebhookHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop((int) Duration.ofSeconds(1).toSeconds());
        }
    }

    private class WebhookHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            if (!isSecretValid(exchange.getRequestHeaders())) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            try (InputStream is = exchange.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                TelegramUpdate update = objectMapper.readValue(body, TelegramUpdate.class);
                handlerUseCase.handle(update);
            } catch (Exception e) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }

        private boolean isSecretValid(Headers headers) {
            String secret = config.getWebhookSecret();
            if (secret == null || secret.isBlank()) {
                return true;
            }
            String incoming = headers.getFirst("X-Telegram-Bot-Api-Secret-Token");
            return secret.equals(incoming);
        }
    }
}
