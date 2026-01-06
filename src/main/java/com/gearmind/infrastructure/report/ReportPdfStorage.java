package com.gearmind.infrastructure.report;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ReportPdfStorage {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ReportPdfStorage() {
    }

    public static Path resolvePath(String reportName) {
        String safeName = (reportName == null || reportName.isBlank()) ? "reporte" : reportName.replaceAll("[^a-zA-Z0-9_-]", "_");
        return baseDir().resolve(String.format("%s_%s.pdf", safeName, FORMATTER.format(LocalDateTime.now())));
    }

    public static Path baseDir() {
        return Paths.get(System.getProperty("user.home"), "GearMind", "documentos", "reportes");
    }
}
