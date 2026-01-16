package com.gearmind.infrastructure.email;

import io.github.cdimascio.dotenv.Dotenv;

public class EmailConfig {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final boolean startTls;
    private final boolean ssl;

    public EmailConfig(String host, int port, String username, String password, String from, boolean startTls, boolean ssl) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from;
        this.startTls = startTls;
        this.ssl = ssl;
    }

    public static EmailConfig fromEnv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String host = dotenv.get("SMTP_HOST", "");
        int port = Integer.parseInt(dotenv.get("SMTP_PORT", "587"));
        String username = dotenv.get("SMTP_USER");
        String password = dotenv.get("SMTP_PASS");
        String from = dotenv.get("SMTP_FROM", username != null ? username : "no-reply@gearmind.local");
        boolean startTls = Boolean.parseBoolean(dotenv.get("SMTP_TLS", "true"));
        boolean ssl = Boolean.parseBoolean(dotenv.get("SMTP_SSL", "false"));
        return new EmailConfig(host, port, username, password, from, startTls, ssl);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFrom() {
        return from;
    }

    public boolean isStartTls() {
        return startTls;
    }

    public boolean isSsl() {
        return ssl;
    }
}
