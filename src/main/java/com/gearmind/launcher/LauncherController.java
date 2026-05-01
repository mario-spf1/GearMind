package com.gearmind.launcher;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.*;
import java.nio.file.*;
import java.sql.*;

public class LauncherController {

    // Setup
    @FXML private VBox setupPanel;
    @FXML private TextField dbHostField;
    @FXML private TextField dbPortField;
    @FXML private TextField dbNameField;
    @FXML private TextField dbUserField;
    @FXML private PasswordField dbPassField;
    @FXML private TextField botTokenField;
    @FXML private Label setupStatusLabel;
    @FXML private Button setupButton;

    // Main
    @FXML private VBox mainPanel;
    @FXML private Label dbStatusLabel;
    @FXML private Label appStatusLabel;
    @FXML private Button appButton;
    @FXML private Label botStatusLabel;
    @FXML private Button botButton;

    private Process appProcess;
    private Process botProcess;

    private static final Path ENV_PATH = Paths.get(".env");

    @FXML
    private void initialize() {
        if (Files.exists(ENV_PATH)) {
            showMainPanel();
        } else {
            showSetupPanel();
        }

        Timeline poller = new Timeline(new KeyFrame(Duration.seconds(2), e -> refreshProcessStatus()));
        poller.setCycleCount(Animation.INDEFINITE);
        poller.play();
    }

    // ─── Setup ────────────────────────────────────────────────────

    private void showSetupPanel() {
        setVisible(setupPanel, true);
        setVisible(mainPanel, false);
        dbHostField.setText("localhost");
        dbPortField.setText("3306");
        dbNameField.setText("gearmind");
        dbUserField.setText("root");
    }

    @FXML
    private void onSaveSetup() {
        String host   = dbHostField.getText().trim();
        String port   = dbPortField.getText().trim();
        String dbName = dbNameField.getText().trim();
        String user   = dbUserField.getText().trim();
        String pass   = dbPassField.getText();
        String token  = botTokenField.getText().trim();

        if (host.isEmpty() || port.isEmpty() || dbName.isEmpty() || user.isEmpty()) {
            setSetupStatus("Rellena todos los campos obligatorios (*).", false);
            return;
        }

        setupButton.setDisable(true);
        setSetupStatus("Comprobando conexión con MySQL...", null);

        new Thread(() -> {
            if (!canConnectMySQL(host, port, user, pass)) {
                Platform.runLater(() -> {
                    setSetupStatus("No se puede conectar a MySQL. Revisa host, puerto y credenciales.", false);
                    setupButton.setDisable(false);
                });
                return;
            }

            Platform.runLater(() -> setSetupStatus("Creando base de datos y aplicando esquema...", null));

            try {
                writeEnvFile(host, port, dbName, user, pass, token);
                runDatabaseSetup();
                Platform.runLater(this::showMainPanel);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setSetupStatus("Error durante la configuración: " + e.getMessage(), false);
                    setupButton.setDisable(false);
                });
            }
        }).start();
    }

    private void setSetupStatus(String msg, Boolean ok) {
        setupStatusLabel.setText(msg == null ? "" : msg);
        setupStatusLabel.getStyleClass().removeAll("tfx-ok", "tfx-error");
        if (Boolean.FALSE.equals(ok)) setupStatusLabel.getStyleClass().add("tfx-error");
        if (Boolean.TRUE.equals(ok))  setupStatusLabel.getStyleClass().add("tfx-ok");
    }

    // ─── Main panel ───────────────────────────────────────────────

    private void showMainPanel() {
        setVisible(setupPanel, false);
        setVisible(mainPanel, true);
        checkDatabaseAsync();
    }

    private void checkDatabaseAsync() {
        setDbBadge("Comprobando...", "tfx-badge-warning");
        appButton.setDisable(true);
        botButton.setDisable(true);

        new Thread(() -> {
            Dotenv env = Dotenv.configure().ignoreIfMissing().load();
            String host   = env.get("DB_HOST", "localhost");
            String port   = env.get("DB_PORT", "3306");
            String dbName = env.get("DB_NAME", "gearmind");
            String user   = env.get("DB_USER", "root");
            String pass   = env.get("DB_PASS", "");
            String token  = env.get("TELEGRAM_BOT_TOKEN", "");

            boolean serverOk = canConnectMySQL(host, port, user, pass);
            boolean dbOk     = serverOk && databaseExists(host, port, dbName, user, pass);

            Platform.runLater(() -> {
                if (!serverOk) {
                    setDbBadge("Sin conexión", "tfx-badge-danger");
                    return;
                }
                if (!dbOk) {
                    setDbBadge("Creando BD...", "tfx-badge-warning");
                    new Thread(() -> {
                        try {
                            runDatabaseSetup();
                            Platform.runLater(() -> {
                                setDbBadge("Conectada", "tfx-badge-success");
                                appButton.setDisable(false);
                                botButton.setDisable(token.isBlank());
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> setDbBadge("Error al crear BD", "tfx-badge-danger"));
                        }
                    }).start();
                    return;
                }
                setDbBadge("Conectada", "tfx-badge-success");
                appButton.setDisable(false);
                botButton.setDisable(token.isBlank());
            });
        }).start();
    }

    private void setDbBadge(String text, String styleClass) {
        dbStatusLabel.setText(text);
        dbStatusLabel.getStyleClass().removeAll("tfx-badge-success", "tfx-badge-danger", "tfx-badge-warning");
        dbStatusLabel.getStyleClass().add(styleClass);
    }

    @FXML
    private void onToggleApp() {
        if (appProcess != null && appProcess.isAlive()) {
            appProcess.destroy();
            appProcess = null;
        } else {
            try {
                appProcess = startProcess("com.gearmind.presentation.App");
            } catch (IOException ignored) {}
        }
        refreshProcessStatus();
    }

    @FXML
    private void onToggleBot() {
        if (botProcess != null && botProcess.isAlive()) {
            botProcess.destroy();
            botProcess = null;
        } else {
            try {
                botProcess = startProcess("com.gearmind.infrastructure.telegram.TelegramWebhookMain");
            } catch (IOException ignored) {}
        }
        refreshProcessStatus();
    }

    private void refreshProcessStatus() {
        boolean appRunning = appProcess != null && appProcess.isAlive();
        boolean botRunning = botProcess != null && botProcess.isAlive();

        updateServiceStatus(appStatusLabel, appButton, appRunning, "Ejecutando", "Detenida");
        updateServiceStatus(botStatusLabel, botButton, botRunning, "Activo", "Inactivo");
    }

    private void updateServiceStatus(Label badge, Button btn, boolean running, String onText, String offText) {
        badge.setText(running ? onText : offText);
        badge.getStyleClass().removeAll("tfx-badge-success", "tfx-badge-danger");
        badge.getStyleClass().add(running ? "tfx-badge-success" : "tfx-badge-danger");
        btn.setText(running ? "Detener" : "Iniciar");
        btn.getStyleClass().removeAll("tfx-btn-primary", "tfx-btn-ghost");
        btn.getStyleClass().add(running ? "tfx-btn-ghost" : "tfx-btn-primary");
    }

    // ─── Utilidades ───────────────────────────────────────────────

    private boolean canConnectMySQL(String host, String port, String user, String pass) {
        String url = "jdbc:mysql://" + host + ":" + port + "/?serverTimezone=UTC&connectTimeout=4000";
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean databaseExists(String host, String port, String dbName, String user, String pass) {
        String url = "jdbc:mysql://" + host + ":" + port + "/information_schema?serverTimezone=UTC";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement(
                     "SELECT SCHEMA_NAME FROM SCHEMATA WHERE SCHEMA_NAME = ?")) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void runDatabaseSetup() throws IOException, InterruptedException {
        Process p = startProcess("com.gearmind.infrastructure.database.DatabaseSetupMain");
        int exit = p.waitFor();
        if (exit != 0) throw new RuntimeException("setup-db terminó con código " + exit);
    }

    private Process startProcess(String mainClass) throws IOException {
        String java = ProcessHandle.current().info().command().orElse("java");
        String cp = "app/GearMind.jar" + File.pathSeparator + "app/lib/*";
        ProcessBuilder pb = new ProcessBuilder(
                java,
                "--module-path", "app/lib",
                "--add-modules", "javafx.controls,javafx.fxml",
                "-cp", cp,
                mainClass);
        pb.directory(new File("."));
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        return pb.start();
    }

    private void writeEnvFile(String host, String port, String dbName, String user, String pass, String token) throws IOException {
        String content = String.join("\n",
                "APP_ENV=development",
                "DB_HOST=" + host,
                "DB_PORT=" + port,
                "DB_NAME=" + dbName,
                "DB_USER=" + user,
                "DB_PASS=" + pass,
                "DB_POOL_MAX=10",
                "TELEGRAM_BOT_TOKEN=" + token,
                "TELEGRAM_WEBHOOK_SECRET=",
                "TELEGRAM_WEBHOOK_PATH=/api/telegram/webhook",
                "TELEGRAM_WEBHOOK_PORT=8081",
                "TELEGRAM_EMPRESA_ID=1",
                ""
        );
        Files.writeString(ENV_PATH, content);
    }

    private static void setVisible(VBox node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    public void onClose() {
        if (appProcess != null && appProcess.isAlive()) appProcess.destroy();
        if (botProcess != null && botProcess.isAlive()) botProcess.destroy();
    }
}
