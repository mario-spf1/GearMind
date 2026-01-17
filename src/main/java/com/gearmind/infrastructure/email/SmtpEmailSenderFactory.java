package com.gearmind.infrastructure.email;

import com.gearmind.domain.email.EmailConfig;
import com.gearmind.domain.email.EmailSender;
import com.gearmind.domain.email.EmailSenderFactory;

public class SmtpEmailSenderFactory implements EmailSenderFactory {

    @Override
    public EmailSender create(EmailConfig config) {
        return new SmtpEmailSender(config);
    }
}
