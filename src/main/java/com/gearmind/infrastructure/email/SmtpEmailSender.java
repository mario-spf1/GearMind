package com.gearmind.infrastructure.email;

import com.gearmind.domain.email.EmailAttachment;
import com.gearmind.domain.email.EmailMessage;
import com.gearmind.domain.email.EmailSender;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SmtpEmailSender implements EmailSender {

    private final EmailConfig config;

    public SmtpEmailSender(EmailConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("La configuración de correo es obligatoria.");
        }
        this.config = config;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            Session session = Session.getInstance(buildProperties(), buildAuthenticator());
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(config.getFrom()));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.getTo(), false));
            mimeMessage.setSubject(message.getSubject(), StandardCharsets.UTF_8.name());
            if (message.getAttachments().isEmpty()) {
                mimeMessage.setText(message.getBody(), StandardCharsets.UTF_8.name(), message.isHtml() ? "html" : "plain");
            } else {
                MimeMultipart multipart = new MimeMultipart();
                MimeBodyPart contentPart = new MimeBodyPart();
                contentPart.setText(message.getBody(), StandardCharsets.UTF_8.name(), message.isHtml() ? "html" : "plain");
                multipart.addBodyPart(contentPart);
                for (EmailAttachment attachment : message.getAttachments()) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(attachment.getPath().toFile());
                    attachmentPart.setFileName(attachment.getFilename());
                    multipart.addBodyPart(attachmentPart);
                }
                mimeMessage.setContent(multipart);
            }
            Transport.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Error enviando el correo.", e);
        } catch (Exception e) {
            throw new RuntimeException("Error preparando el correo.", e);
        }
    }

    private Properties buildProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.getHost());
        props.put("mail.smtp.port", Integer.toString(config.getPort()));
        props.put("mail.smtp.auth", config.getUsername() != null && !config.getUsername().isBlank());
        if (config.isStartTls()) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        if (config.isSsl()) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        return props;
    }

    private Authenticator buildAuthenticator() {
        if (config.getUsername() == null || config.getUsername().isBlank()) {
            return null;
        }
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUsername(), config.getPassword());
            }
        };
    }
}
