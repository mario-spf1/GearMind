package com.gearmind.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
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
                url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?serverTimezone=UTC&characterEncoding=UTF-8";
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
}
