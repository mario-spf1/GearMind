package com.gearmind.domain.email;

public interface EmailSenderFactory {

    EmailSender create(EmailConfig config);
}
