package com.gearmind.domain.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EmailMessage {

    private final String to;
    private final String subject;
    private final String body;
    private final boolean html;
    private final List<EmailAttachment> attachments;

    public EmailMessage(String to, String subject, String body) {
        this(to, subject, body, false, List.of());
    }

    public EmailMessage(String to, String subject, String body, boolean html, List<EmailAttachment> attachments) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("El destinatario es obligatorio.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("El asunto es obligatorio.");
        }
        this.to = to;
        this.subject = subject;
        this.body = Objects.requireNonNull(body, "El cuerpo del correo es obligatorio.");
        this.html = html;
        if (attachments == null || attachments.isEmpty()) {
            this.attachments = List.of();
        } else {
            this.attachments = Collections.unmodifiableList(new ArrayList<>(attachments));
        }
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public boolean isHtml() {
        return html;
    }

    public List<EmailAttachment> getAttachments() {
        return attachments;
    }
}
