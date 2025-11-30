package com.gearmind.presentation.controller;

import com.gearmind.application.common.SessionManager;
import com.gearmind.domain.user.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {

    @FXML
    private Label lblSubtitle;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();

        if (user != null) {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            lblSubtitle.setText("Bienvenido, " + user.getNombre() + " · Empresa " + empresaId + " · Rol " + user.getRol());
        } else {
            lblSubtitle.setText("Gestión de talleres · GearMind");
        }
    }

    @FXML
    public void onNewProject() {
        System.out.println("Nuevo proyecto (placeholder)");
    }

    @FXML
    public void onOpen() {
        System.out.println("Abrir (placeholder)");
    }

    @FXML
    private void onLogout() {
        try {
            SessionManager.getInstance().clearSession();

            Stage stage = (Stage) lblSubtitle.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/LoginView.fxml")
            );
            Parent root = loader.load();

            Scene scene = new Scene(
                    root,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight()
            );

            scene.getStylesheets().add(
                    getClass().getResource("/styles/theme.css").toExternalForm()
            );
            scene.getStylesheets().add(
                    getClass().getResource("/styles/components.css").toExternalForm()
            );

            stage.setTitle("GearMind — Acceso");
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
