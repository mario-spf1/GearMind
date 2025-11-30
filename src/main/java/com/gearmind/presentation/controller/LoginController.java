package com.gearmind.presentation.controller;

import com.gearmind.application.auth.*;
import com.gearmind.application.common.SessionManager;
import com.gearmind.domain.user.User;
import com.gearmind.infrastructure.auth.BCryptPasswordHasher;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    private final LoginUseCase loginUseCase;

    public LoginController() {
        this.loginUseCase = new LoginUseCase(
                new MySqlUserRepository(),
                new BCryptPasswordHasher()
        );
    }

    @FXML
    private void onLogin() {
        String emailOrUser = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (emailOrUser.isEmpty() || password.isEmpty()) {
            showError("Debes introducir usuario y contraseña.");
            return;
        }

        try {
            LoginRequest request = new LoginRequest(emailOrUser, password);
            LoginResponse response = loginUseCase.login(request);

            User loggedUser = response.user();

            SessionManager.getInstance().startSession(loggedUser);

            goToHome();

        } catch (InvalidCredentialsException e) {
            showError("Usuario o contraseña incorrectos.");
        } catch (InactiveUserException e) {
            showError("El usuario está inactivo.");
        } catch (IOException e) {
            showError("Se ha producido un error al iniciar sesión.");
        }
    }

    private void goToHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/HomeView.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) loginButton.getScene().getWindow();

        Scene scene = new Scene(root,
                stage.getScene().getWidth(),
                stage.getScene().getHeight());

        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());

        stage.setTitle("GearMind — Inicio");
        stage.setScene(scene);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de acceso");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
