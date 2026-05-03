package com.gearmind.infrastructure.telegram;

import io.github.cdimascio.dotenv.Dotenv;

public class TelegramConfig {

    private final String botToken;
    private final long empresaId;

    public TelegramConfig() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.botToken = readValue(dotenv, "TELEGRAM_BOT_TOKEN", null);
        this.empresaId = Long.parseLong(readValue(dotenv, "TELEGRAM_EMPRESA_ID", "1"));
    }

    public String getBotToken() {
        return botToken;
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
