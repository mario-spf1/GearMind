package com.gearmind.presentation.controller;

import com.gearmind.application.auth.InactiveUserException;
import com.gearmind.application.auth.InvalidCredentialsException;
import com.gearmind.application.auth.LoginRequest;
import com.gearmind.application.auth.LoginResponse;
import com.gearmind.application.auth.LoginUseCase;
import com.gearmind.application.email.EnviarEmailEmpresaUseCase;
import com.gearmind.application.email.SendPasswordRecoveryEmailRequest;
import com.gearmind.application.email.SendPasswordRecoveryEmailUseCase;
import com.gearmind.application.common.SessionManager;
import com.gearmind.common.exception.ValidationException;
import com.gearmind.domain.user.User;
import com.gearmind.infrastructure.auth.BCryptPasswordHasher;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import com.gearmind.infrastructure.company.MySqlEmpresaRepository;
import com.gearmind.infrastructure.email.JdbcSmtpConfigRepository;
import com.gearmind.infrastructure.email.SmtpEmailSenderFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Hyperlink forgotPasswordLink;

    private final LoginUseCase loginUseCase;
    private final SendPasswordRecoveryEmailUseCase sendPasswordRecoveryEmailUseCase;

    public LoginController() {
        MySqlUserRepository userRepository = new MySqlUserRepository();
        BCryptPasswordHasher passwordHasher = new BCryptPasswordHasher();
        this.loginUseCase = new LoginUseCase(userRepository, passwordHasher, new MySqlEmpresaRepository());
        EnviarEmailEmpresaUseCase enviarEmailEmpresaUseCase = new EnviarEmailEmpresaUseCase(new JdbcSmtpConfigRepository(), new SmtpEmailSenderFactory());
        this.sendPasswordRecoveryEmailUseCase = new SendPasswordRecoveryEmailUseCase(userRepository, enviarEmailEmpresaUseCase, passwordHasher);
    }

    @FXML
    private void initialize() {
        if (loginButton != null) {
            loginButton.setDefaultButton(true);
        }
        if (passwordField != null) {
            passwordField.setOnAction(e -> onLogin());
        }
    }

    @FXML
    private void onLogin() {
        String emailOrUser = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText().trim() : "";

        if (emailOrUser.isEmpty() || password.isEmpty()) {
            showError("Debes introducir usuario y contraseña.");
            return;
        }

        try {
            loginButton.setDisable(true);
            LoginRequest request = new LoginRequest(emailOrUser, password);
            LoginResponse response = loginUseCase.login(request);
            User loggedUser = response.user();
            String empresaNombre = response.empresaNombre();
            SessionManager.getInstance().startSession(loggedUser, empresaNombre);
            goToHome();

        } catch (InvalidCredentialsException e) {
            showError("Usuario o contraseña incorrectos.");
        } catch (InactiveUserException e) {
            showError("El usuario está inactivo.");
        } catch (IOException e) {
            e.printStackTrace();
            showError("No se ha podido cargar la pantalla principal.");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Se ha producido un error inesperado al iniciar sesión.");
        } finally {
            loginButton.setDisable(false);
            passwordField.clear();
        }
    }

    @FXML
    private void onForgotPassword() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Recuperar contraseña");
        dialog.setHeaderText("Introduce tu email para recibir una contraseña temporal.");
        dialog.setContentText("Email:");

        dialog.showAndWait().ifPresent(emailInput -> {
            String email = emailInput != null ? emailInput.trim() : "";
            if (email.isBlank()) {
                showWarning("Debes introducir un email.");
                return;
            }
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                showWarning("El email no tiene un formato válido.");
                return;
            }
            try {
                sendPasswordRecoveryEmailUseCase.execute(new SendPasswordRecoveryEmailRequest(email));
                new Alert(Alert.AlertType.INFORMATION, "Se ha enviado una contraseña temporal a " + email + ".").showAndWait();
            } catch (ValidationException ex) {
                String msg = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("smtp")
                        ? "El envío de emails aún no está configurado. Pide a tu administrador que lo configure desde Ajustes."
                        : ex.getMessage();
                showWarning(msg);
            } catch (Exception ex) {
                showError("No se pudo enviar el correo de recuperación. Inténtalo más tarde.");
            }
        });
    }

    private void goToHome() throws IOException {
        URL fxml = getClass().getResource("/view/HomeView.fxml");
        if (fxml == null) {
            throw new IOException("No se encuentra el recurso /view/HomeView.fxml en el classpath");
        }

        FXMLLoader loader = new FXMLLoader(fxml);
        Parent root = loader.load();
        Stage stage = (Stage) loginButton.getScene().getWindow();
        double width = stage.getScene() != null ? stage.getScene().getWidth() : 1024;
        double height = stage.getScene() != null ? stage.getScene().getHeight() : 720;
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
        stage.setTitle("GearMind — Inicio");
        stage.setScene(scene);
        stage.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de acceso");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
