package com.gearmind.tools;

import com.gearmind.infrastructure.security.AesCipher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public final class SmtpPasswordCipherTool {

    private SmtpPasswordCipherTool() {
    }

    public static void main(String[] args) throws IOException {

        String password;

        if (args.length > 0 && !args[0].isBlank()) {
            password = args[0].trim();
        } else {
            System.out.print("Introduce la contraseña SMTP: ");
            password = new Scanner(System.in).nextLine().trim();
        }

        if (password.isEmpty()) {
            System.err.println("❌ La contraseña no puede estar vacía.");
            System.exit(1);
        }

        AesCipher cipher = new AesCipher();
        String encrypted = cipher.encrypt(password);

        Path outputFile = Path.of("smtp_password.enc");
        Files.writeString(outputFile, encrypted);

        System.out.println("✅ Contraseña cifrada correctamente.");
        System.out.println("📄 Archivo generado: " + outputFile.toAbsolutePath());
    }
}
