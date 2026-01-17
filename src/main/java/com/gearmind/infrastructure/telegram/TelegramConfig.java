package com.gearmind.infrastructure.telegram;

import io.github.cdimascio.dotenv.Dotenv;

public class TelegramConfig {

    private final String botToken;
    private final String webhookSecret;
    private final String webhookPath;
    private final int webhookPort;
    private final long empresaId;

    public TelegramConfig() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.botToken = readValue(dotenv, "TELEGRAM_BOT_TOKEN", null);
        this.webhookSecret = readValue(dotenv, "TELEGRAM_WEBHOOK_SECRET", "");
        this.webhookPath = readValue(dotenv, "TELEGRAM_WEBHOOK_PATH", "/api/telegram/webhook");
        this.webhookPort = Integer.parseInt(readValue(dotenv, "TELEGRAM_WEBHOOK_PORT", "8081"));
        this.empresaId = Long.parseLong(readValue(dotenv, "TELEGRAM_EMPRESA_ID", "1"));
    }

    public String getBotToken() {
        return botToken;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public String getWebhookPath() {
        return webhookPath;
    }

    public int getWebhookPort() {
        return webhookPort;
    }

    public long getEmpresaId() {
        return empresaId;
    }

    private String readValue(Dotenv dotenv, String key, String fallback) {
        String fromDotenv = dotenv.get(key);
        if (fromDotenv != null) {
            return fromDotenv;
        }
        String fromEnv = System.getenv(key);
        return fromEnv != null ? fromEnv : fallback;
    }
}
