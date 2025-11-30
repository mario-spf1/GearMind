package com.gearmind.presentation.controller;

import com.gearmind.application.auth.InactiveUserException;
import com.gearmind.application.auth.InvalidCredentialsException;
import com.gearmind.application.auth.LoginRequest;
import com.gearmind.application.auth.LoginResponse;
import com.gearmind.application.auth.LoginUseCase;
import com.gearmind.domain.user.User;
import com.gearmind.infrastructure.auth.InMemoryUserRepository;
import com.gearmind.infrastructure.auth.PlainTextPasswordHasher;
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

    @FXML
    private Hyperlink forgotPasswordLink;

    // Caso de uso de login
    private final LoginUseCase loginUseCase;

    public LoginController() {
        // De momento usamos la implementación en memoria
        this.loginUseCase = new LoginUseCase(
                new InMemoryUserRepository(),
                new PlainTextPasswordHasher()
        );
    }

    @FXML
    private void initialize() {
        loginButton.setDefaultButton(true);
        passwordField.setOnAction(e -> onLogin());
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
            // De momento tratamos el campo USUARIO como email
            LoginRequest request = new LoginRequest(emailOrUser, password);
            LoginResponse response = loginUseCase.login(request);

            User loggedUser = response.user();
            goToHome(loggedUser);

        } catch (InvalidCredentialsException e) {
            showError("Usuario o contraseña incorrectos.");
        } catch (InactiveUserException e) {
            showError("El usuario está inactivo.");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Se ha producido un error al iniciar sesión.");
        }
    }

    private void goToHome(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/HomeView.fxml"));
        Parent root = loader.load();

        HomeController homeController = loader.getController();
        homeController.setCurrentUser(user);

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
