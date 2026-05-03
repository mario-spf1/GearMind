package com.gearmind.infrastructure.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gearmind.infrastructure.telegram.dto.TelegramUpdate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
        return sendMessage(chatId, text, null);
    }

    public boolean sendMessage(long chatId, String text, Map<String, Object> replyMarkup) {
        if (config.getBotToken() == null || config.getBotToken().isBlank()) {
            return false;
        }

        try {
            Map<String, Object> payloadMap = new java.util.HashMap<>();
            payloadMap.put("chat_id", chatId);
            payloadMap.put("text", text);
            if (replyMarkup != null) {
                payloadMap.put("reply_markup", replyMarkup);
            }
            HttpResponse<String> response = postJson("sendMessage", payloadMap, Duration.ofSeconds(10));
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean sendMessageWithKeyboard(long chatId, String text, java.util.List<java.util.List<String>> keyboardRows, boolean oneTime) {
        Map<String, Object> replyMarkup = Map.of(
                "keyboard", keyboardRows,
                "resize_keyboard", true,
                "one_time_keyboard", oneTime
        );
        return sendMessage(chatId, text, replyMarkup);
    }

    /**
     * Borra el webhook que pudiera haber configurado una versión previa del bot.
     * Necesario antes de hacer long polling: si hay un webhook activo, getUpdates
     * devuelve 409 Conflict.
     */
    public boolean deleteWebhook() {
        if (config.getBotToken() == null || config.getBotToken().isBlank()) {
            return false;
        }
        try {
            HttpResponse<String> response = postJson("deleteWebhook", Map.of("drop_pending_updates", false), Duration.ofSeconds(10));
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Long polling. Bloquea hasta {@code timeoutSeconds} esperando updates nuevos.
     * Devuelve lista vacía si no hay novedades o si la llamada falla; el caller
     * decide si reintentar.
     */
    public List<TelegramUpdate> getUpdates(long offset, int timeoutSeconds) throws Exception {
        if (config.getBotToken() == null || config.getBotToken().isBlank()) {
            return List.of();
        }
        Map<String, Object> payload = new java.util.HashMap<>();
        if (offset > 0) {
            payload.put("offset", offset);
        }
        payload.put("timeout", timeoutSeconds);
        // Damos margen al read-timeout sobre el long-poll para que el servidor responda primero.
        Duration readTimeout = Duration.ofSeconds(timeoutSeconds + 10L);

        HttpResponse<String> response = postJson("getUpdates", payload, readTimeout);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("getUpdates HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (!root.path("ok").asBoolean(false)) {
            throw new RuntimeException("getUpdates not ok: " + response.body());
        }
        JsonNode result = root.path("result");
        List<TelegramUpdate> updates = new ArrayList<>();
        if (result.isArray()) {
            for (JsonNode node : result) {
                updates.add(objectMapper.treeToValue(node, TelegramUpdate.class));
            }
        }
        return updates;
    }

    private HttpResponse<String> postJson(String method, Object payload, Duration timeout) throws Exception {
        String url = "https://api.telegram.org/bot" + config.getBotToken() + "/" + method;
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
