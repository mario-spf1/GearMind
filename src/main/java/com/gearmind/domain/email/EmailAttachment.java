package com.gearmind.domain.email;

import java.nio.file.Path;
import java.util.Objects;

public class EmailAttachment {

    private final String filename;
    private final Path path;
    private final String contentType;

    public EmailAttachment(String filename, Path path, String contentType) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("El nombre del adjunto es obligatorio.");
        }
        this.filename = filename;
        this.path = Objects.requireNonNull(path, "El adjunto necesita una ruta válida.");
        this.contentType = contentType;
    }

    public String getFilename() {
        return filename;
    }

    public Path getPath() {
        return path;
    }

    public String getContentType() {
        return contentType;
    }
}
