package com.gearmind.domain.email;

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
