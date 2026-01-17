package com.gearmind.infrastructure.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class TelegramBotClient {

    private final TelegramConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TelegramBotClient(TelegramConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
    }

    public boolean sendMessage(long chatId, String text) {
        if (config.getBotToken() == null || config.getBotToken().isBlank()) {
            return false;
        }

        try {
            String url = "https://api.telegram.org/bot" + config.getBotToken() + "/sendMessage";
            String payload = objectMapper.writeValueAsString(Map.of("chat_id", chatId, "text", text));

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }
}
