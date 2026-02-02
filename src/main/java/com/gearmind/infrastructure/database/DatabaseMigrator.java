package com.gearmind.infrastructure.database;

import org.flywaydb.core.Flyway;

public final class DatabaseMigrator {

    private DatabaseMigrator() {
    }

    public static void migrate() {
        Flyway.configure().dataSource(DataSourceFactory.getDataSource()).load().migrate();
    }
}
