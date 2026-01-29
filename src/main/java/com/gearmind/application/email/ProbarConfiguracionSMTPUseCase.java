package com.gearmind.application.email;

import com.gearmind.domain.email.EmailMessage;
import com.gearmind.common.exception.ValidationException;

public class ProbarConfiguracionSMTPUseCase {

    private final EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase;

    public ProbarConfiguracionSMTPUseCase(EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase) {
        this.enviarEmailEmpresaUseCase = enviarEmailEmpresaUseCase;
    }

    public void execute(long empresaId, String destinatario) {
        if (destinatario == null || destinatario.isBlank()) {
            throw new ValidationException("El destinatario de prueba es obligatorio.");
        }
        EmailMessage message = new EmailMessage(
                destinatario.trim(),
                "Prueba de configuración SMTP",
                "Hola,\n\nEste es un correo de prueba para confirmar la configuración SMTP de GearMind.\n\nUn saludo,\nGearMind"
        );
        enviarEmailEmpresaUseCase.execute(empresaId, message);
    }
}
