package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.common.exception.ValidationException;
import com.gearmind.domain.email.EmailConfig;
import com.gearmind.domain.email.EmailMessage;
import com.gearmind.domain.email.SmtpConfigRepository;
import com.gearmind.infrastructure.email.JdbcSmtpConfigRepository;
import com.gearmind.infrastructure.email.SmtpEmailSender;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

public class AjustesController {

    private static final Path ENV_PATH = Paths.get(".env");

    @FXML private TextField smtpHostField;
    @FXML private TextField smtpPortField;
    @FXML private TextField smtpUsernameField;
    @FXML private PasswordField smtpPasswordField;
    @FXML private TextField smtpFromField;
    @FXML private CheckBox smtpStartTlsCheck;
    @FXML private CheckBox smtpSslCheck;
    @FXML private TextField smtpTestRecipientField;
    @FXML private Label smtpStatusLabel;

    @FXML private TextField botTokenField;
    @FXML private TextField botEmpresaIdField;
    @FXML private Label botStatusLabel;

    private final SmtpConfigRepository smtpRepo = new JdbcSmtpConfigRepository();

    @FXML
    private void initialize() {
        if (!AuthContext.isLoggedIn() || !AuthContext.isAdminOrSuperAdmin()) {
            setSmtpStatus("No tienes permisos para acceder a Ajustes.", false);
            setBotStatus("No tienes permisos para acceder a Ajustes.", false);
            return;
        }
        loadSmtp();
        loadTelegram();
    }

    // ─── SMTP ──────────────────────────────────────────────────

    private void loadSmtp() {
        try {
            EmailConfig cfg = smtpRepo.findByEmpresaId(AuthContext.getEmpresaId());
            smtpHostField.setText(cfg.getHost());
            smtpPortField.setText(String.valueOf(cfg.getPort()));
            smtpUsernameField.setText(nullSafe(cfg.getUsername()));
            smtpPasswordField.setText(nullSafe(cfg.getPassword()));
            smtpFromField.setText(nullSafe(cfg.getFrom()));
            smtpStartTlsCheck.setSelected(cfg.isStartTls());
            smtpSslCheck.setSelected(cfg.isSsl());
            setSmtpStatus("Configuración cargada.", true);
        } catch (ValidationException e) {
            setSmtpStatus("Aún no hay configuración SMTP guardada para esta empresa.", null);
        } catch (Exception e) {
            setSmtpStatus("Error cargando la configuración: " + e.getMessage(), false);
        }
    }

    @FXML
    private void onSaveSmtp() {
        try {
            EmailConfig cfg = readSmtpForm();
            long empresaId = AuthContext.getEmpresaId();
            if (existsSmtp(empresaId)) {
                smtpRepo.update(empresaId, cfg);
            } else {
                smtpRepo.save(empresaId, cfg);
            }
            setSmtpStatus("Configuración SMTP guardada correctamente.", true);
        } catch (ValidationException ve) {
            setSmtpStatus(ve.getMessage(), false);
        } catch (Exception e) {
            setSmtpStatus("Error guardando: " + e.getMessage(), false);
        }
    }

    @FXML
    private void onTestSmtp() {
        String to = trim(smtpTestRecipientField.getText());
        if (to.isEmpty()) {
            setSmtpStatus("Introduce un email destinatario para la prueba.", false);
            return;
        }
        try {
            EmailConfig cfg = readSmtpForm();
            SmtpEmailSender sender = new SmtpEmailSender(cfg);
            sender.send(new EmailMessage(to,
                    "GearMind — Prueba SMTP",
                    "Este es un email de prueba enviado desde GearMind. Si lo recibes, la configuración SMTP funciona correctamente."));
            setSmtpStatus("Email de prueba enviado a " + to + ".", true);
        } catch (ValidationException ve) {
            setSmtpStatus(ve.getMessage(), false);
        } catch (Exception e) {
            setSmtpStatus("Error enviando prueba: " + e.getMessage(), false);
        }
    }

    private boolean existsSmtp(long empresaId) {
        try {
            smtpRepo.findByEmpresaId(empresaId);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    private EmailConfig readSmtpForm() {
        String host = trim(smtpHostField.getText());
        String portStr = trim(smtpPortField.getText());
        String user = trim(smtpUsernameField.getText());
        String pass = smtpPasswordField.getText() == null ? "" : smtpPasswordField.getText();
        String from = trim(smtpFromField.getText());

        if (host.isEmpty()) throw new ValidationException("El host es obligatorio.");
        if (portStr.isEmpty()) throw new ValidationException("El puerto es obligatorio.");
        if (from.isEmpty()) throw new ValidationException("La dirección de envío (From) es obligatoria.");

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new ValidationException("El puerto debe ser un número.");
        }

        return new EmailConfig(host, port, user, pass, from,
                smtpStartTlsCheck.isSelected(), smtpSslCheck.isSelected());
    }

    private void setSmtpStatus(String msg, Boolean ok) {
        applyStatus(smtpStatusLabel, msg, ok);
    }

    // ─── Telegram (.env) ───────────────────────────────────────

    private void loadTelegram() {
        try {
            Map<String, String> env = readEnv();
            botTokenField.setText(env.getOrDefault("TELEGRAM_BOT_TOKEN", ""));
            String empresaIdDefault = String.valueOf(AuthContext.getEmpresaId());
            botEmpresaIdField.setText(env.getOrDefault("TELEGRAM_EMPRESA_ID", empresaIdDefault));
            setBotStatus("Configuración cargada.", true);
        } catch (Exception e) {
            setBotStatus("Error cargando .env: " + e.getMessage(), false);
        }
    }

    @FXML
    private void onSaveTelegram() {
        try {
            String token = trim(botTokenField.getText());
            String empresaIdStr = trim(botEmpresaIdField.getText());

            if (!empresaIdStr.isEmpty()) {
                try { Long.parseLong(empresaIdStr); }
                catch (NumberFormatException e) { throw new ValidationException("El empresa ID debe ser un número."); }
            }

            Map<String, String> updates = new LinkedHashMap<>();
            updates.put("TELEGRAM_BOT_TOKEN", token);
            updates.put("TELEGRAM_EMPRESA_ID", empresaIdStr);
            writeEnv(updates);

            setBotStatus("Guardado. Reinicia el bot desde el launcher para aplicar los cambios.", true);
        } catch (ValidationException ve) {
            setBotStatus(ve.getMessage(), false);
        } catch (Exception e) {
            setBotStatus("Error guardando: " + e.getMessage(), false);
        }
    }

    private Map<String, String> readEnv() throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        if (!Files.exists(ENV_PATH)) return map;
        for (String line : Files.readAllLines(ENV_PATH)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int idx = line.indexOf('=');
            if (idx <= 0) continue;
            map.put(line.substring(0, idx).trim(), line.substring(idx + 1));
        }
        return map;
    }

    private void writeEnv(Map<String, String> updates) throws IOException {
        List<String> lines = Files.exists(ENV_PATH)
                ? new ArrayList<>(Files.readAllLines(ENV_PATH))
                : new ArrayList<>();
        Set<String> pendingKeys = new LinkedHashSet<>(updates.keySet());

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int idx = line.indexOf('=');
            if (idx <= 0) continue;
            String key = line.substring(0, idx).trim();
            if (updates.containsKey(key)) {
                lines.set(i, key + "=" + updates.get(key));
                pendingKeys.remove(key);
            }
        }
        for (String key : pendingKeys) {
            lines.add(key + "=" + updates.get(key));
        }
        Files.write(ENV_PATH, lines);
    }

    private void setBotStatus(String msg, Boolean ok) {
        applyStatus(botStatusLabel, msg, ok);
    }

    // ─── Utilidades ───────────────────────────────────────────

    private static void applyStatus(Label label, String msg, Boolean ok) {
        if (label == null) return;
        label.setText(msg == null ? "" : msg);
        label.getStyleClass().removeAll("tfx-ok", "tfx-error", "tfx-muted");
        if (Boolean.TRUE.equals(ok)) label.getStyleClass().add("tfx-ok");
        else if (Boolean.FALSE.equals(ok)) label.getStyleClass().add("tfx-error");
        else label.getStyleClass().add("tfx-muted");
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
