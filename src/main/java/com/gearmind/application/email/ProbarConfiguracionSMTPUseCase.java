package com.gearmind.application.email;

import com.gearmind.domain.email.EmailMessage;

public class ProbarConfiguracionSMTPUseCase {

    private final EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase;

    public ProbarConfiguracionSMTPUseCase(EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase) {
        this.enviarEmailEmpresaUseCase = enviarEmailEmpresaUseCase;
    }

    public void execute(long empresaId, String destinatario) {
        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException("El destinatario de prueba es obligatorio.");
        }
        EmailMessage message = new EmailMessage(
                destinatario.trim(),
                "Prueba de configuración SMTP",
                "Este es un correo de prueba de la configuración SMTP de GearMind."
        );
        enviarEmailEmpresaUseCase.execute(empresaId, message);
    }
}
