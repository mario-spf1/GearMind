package com.gearmind.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public final class DataSourceFactory {

    private static HikariDataSource dataSource;

    private DataSourceFactory() {
    }

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String url = dotenv.get("DB_URL");

            if (url == null || url.isBlank()) {
                String host = dotenv.get("DB_HOST", "localhost");
                String port = dotenv.get("DB_PORT", "3306");
                String dbName = dotenv.get("DB_NAME", "gearmind");
                ensureDatabaseExists(host, port, dbName, dotenv.get("DB_USER", "root"), dotenv.get("DB_PASS", ""));
                url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?serverTimezone=UTC&characterEncoding=UTF-8";
            } else {
                DatabaseTarget target = parseTarget(url);
                if (target != null && target.dbName() != null && !target.dbName().isBlank()) {
                    ensureDatabaseExists(target.host(), target.port(), target.dbName(), dotenv.get("DB_USER", "root"), dotenv.get("DB_PASS", ""));
                }
            }

            String user = dotenv.get("DB_USER", "root");
            String pass = dotenv.get("DB_PASS", "");
            int maxPool = Integer.parseInt(dotenv.get("DB_POOL_MAX", "10"));

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);
            config.setMaximumPoolSize(maxPool);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setPoolName("gearmind-pool");

            dataSource = new HikariDataSource(config);
        }

        return dataSource;
    }

    private static void ensureDatabaseExists(String host, String port, String dbName, String user, String pass) {
        if (dbName == null || dbName.isBlank()) {
            return;
        }
        String baseUrl = "jdbc:mysql://" + host + ":" + port + "/?serverTimezone=UTC&characterEncoding=UTF-8";
        String sql = "CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        try (Connection conn = DriverManager.getConnection(baseUrl, user, pass); Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error creando la base de datos " + dbName, e);
        }
    }

    private static DatabaseTarget parseTarget(String url) {
        String prefix = "jdbc:mysql://";
        if (!url.startsWith(prefix)) {
            return null;
        }
        String remaining = url.substring(prefix.length());
        String[] hostAndPath = remaining.split("/", 2);
        if (hostAndPath.length < 2) {
            return null;
        }
        String hostPort = hostAndPath[0];
        String path = hostAndPath[1];
        String dbName = path.split("\\?", 2)[0];
        if (dbName.isBlank()) {
            return null;
        }
        String host;
        String port = "3306";
        if (hostPort.contains(":")) {
            String[] parts = hostPort.split(":", 2);
            host = parts[0];
            if (!parts[1].isBlank()) {
                port = parts[1];
            }
        } else {
            host = hostPort;
        }
        if (host.isBlank()) {
            return null;
        }
        return new DatabaseTarget(host, port, dbName);
    }

    private record DatabaseTarget(String host, String port, String dbName) {

    }
}
