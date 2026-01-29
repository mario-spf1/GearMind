package com.gearmind.application.email;

import com.gearmind.common.exception.ValidationException;
import com.gearmind.domain.email.EmailMessage;
import com.gearmind.domain.security.PasswordHasher;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRepository;
import java.security.SecureRandom;

public class SendPasswordRecoveryEmailUseCase {

    private final UserRepository userRepository;
    private final EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase;
    private final PasswordHasher passwordHasher;
    private final SecureRandom secureRandom = new SecureRandom();

    public SendPasswordRecoveryEmailUseCase(UserRepository userRepository, EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.enviarEmailEmpresaUseCase = enviarEmailEmpresaUseCase;
        this.passwordHasher = passwordHasher;
    }

    public void execute(SendPasswordRecoveryEmailRequest request) {
        if (request == null) {
            throw new ValidationException("La solicitud de recuperación no puede ser nula.");
        }
        String email = safeTrim(request.email());
        if (email.isBlank()) {
            throw new ValidationException("El email es obligatorio.");
        }

        User user = userRepository.findByEmail(email.toLowerCase()).orElseThrow(() -> new ValidationException("No existe un usuario con ese email."));
        String temporaryPassword = generateTemporaryPassword();
        String hashedPassword = passwordHasher.hash(temporaryPassword);
        userRepository.update(user.getId(), user.getEmpresaId(), user.getNombre(), user.getEmail(), hashedPassword, user.getRol(), user.isActivo());

        StringBuilder body = new StringBuilder();
        body.append("Hola ").append(user.getNombre()).append(",\n\n");
        body.append("Hemos recibido una solicitud para restablecer tu contraseña en GearMind.\n\n");
        body.append("Tu contraseña temporal es:\n");
        body.append(temporaryPassword).append("\n\n");
        body.append("Por seguridad, cambia la contraseña en cuanto accedas al sistema.\n");
        body.append("Si no solicitaste este cambio, ignora este correo y contacta con el administrador.\n");

        EmailMessage message = new EmailMessage(user.getEmail(), "Recuperación de contraseña - credenciales temporales", body.toString());
        enviarEmailEmpresaUseCase.execute(user.getEmpresaId(), message);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String generateTemporaryPassword() {
        String charset = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
        int length = 10;
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(charset.charAt(secureRandom.nextInt(charset.length())));
        }
        return builder.toString();
    }
}
