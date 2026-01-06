package com.gearmind.infrastructure.document;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class RepairDocumentStorage {

    private RepairDocumentStorage() {
    }

    public static Path baseDir() {
        return Paths.get(System.getProperty("user.home"), "GearMind", "documentos", "reparaciones");
    }

    public static Path resolveDir(long repairId) {
        return baseDir().resolve(String.valueOf(repairId));
    }
}
