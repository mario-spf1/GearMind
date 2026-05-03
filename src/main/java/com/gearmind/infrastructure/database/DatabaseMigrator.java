package com.gearmind.infrastructure.database;

import org.flywaydb.core.Flyway;

public final class DatabaseMigrator {

    private DatabaseMigrator() {
    }

    public static void migrate() {
        Flyway.configure().dataSource(DataSourceFactory.getDataSource()).locations("classpath:db/migration").baselineOnMigrate(true).load().migrate();
    }
}
