package com.gearmind.application.email;

import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRepository;
import com.gearmind.domain.email.EmailMessage;

public class SendPasswordRecoveryEmailUseCase {

    private final UserRepository userRepository;
    private final EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase;

    public SendPasswordRecoveryEmailUseCase(UserRepository userRepository, EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase) {
        this.userRepository = userRepository;
        this.enviarEmailEmpresaUseCase = enviarEmailEmpresaUseCase;
    }

    public void execute(SendPasswordRecoveryEmailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de recuperación no puede ser nula.");
        }
        String email = safeTrim(request.email());
        if (email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }
        if ((request.recoveryCode() == null || request.recoveryCode().isBlank())
                && (request.recoveryUrl() == null || request.recoveryUrl().isBlank())) {
            throw new IllegalArgumentException("Debes indicar un código o un enlace de recuperación.");
        }

        User user = userRepository.findByEmail(email.toLowerCase()).orElseThrow(() -> new IllegalArgumentException("No existe un usuario con ese email."));

        StringBuilder body = new StringBuilder();
        body.append("Hola ").append(user.getNombre()).append(",\n\n");
        body.append("Hemos recibido una solicitud para recuperar tu contraseña en GearMind.\n\n");
        if (request.recoveryCode() != null && !request.recoveryCode().isBlank()) {
            body.append("Código de recuperación: ").append(request.recoveryCode().trim()).append("\n");
        }
        if (request.recoveryUrl() != null && !request.recoveryUrl().isBlank()) {
            body.append("Enlace para restablecer la contraseña: ").append(request.recoveryUrl().trim()).append("\n");
        }
        body.append("\nSi no solicitaste este cambio, ignora este correo.\n");

        EmailMessage message = new EmailMessage(user.getEmail(), "Recuperación de contraseña", body.toString());
        enviarEmailEmpresaUseCase.execute(user.getEmpresaId(), message);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
