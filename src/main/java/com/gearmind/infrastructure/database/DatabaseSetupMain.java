package com.gearmind.infrastructure.database;

public final class DatabaseSetupMain {

    private DatabaseSetupMain() {
    }

    public static void main(String[] args) {
        DatabaseMigrator.migrate();
        System.out.println("Database migrations completed.");
    }
}
