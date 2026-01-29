package com.gearmind.application.email;

import com.gearmind.common.exception.ValidationException;
import com.gearmind.domain.email.EmailConfig;
import com.gearmind.domain.email.EmailMessage;
import com.gearmind.domain.email.EmailSender;
import com.gearmind.domain.email.EmailSenderFactory;
import com.gearmind.domain.email.SmtpConfigRepository;

public class EnviarEmailEmpresaUseCase {

    private final SmtpConfigRepository smtpConfigRepository;
    private final EmailSenderFactory emailSenderFactory;

    public EnviarEmailEmpresaUseCase(SmtpConfigRepository smtpConfigRepository, EmailSenderFactory emailSenderFactory) {
        this.smtpConfigRepository = smtpConfigRepository;
        this.emailSenderFactory = emailSenderFactory;
    }

    public void execute(long empresaId, EmailMessage message) {
        if (message == null) {
            throw new ValidationException("El mensaje de correo es obligatorio.");
        }
        EmailConfig config = smtpConfigRepository.findByEmpresaId(empresaId);
        EmailSender sender = emailSenderFactory.create(config);
        sender.send(message);
    }
}
