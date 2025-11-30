package com.gearmind.presentation.controller;

import com.gearmind.domain.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController {

    @FXML
    private Label lblSubtitle;

    @FXML
    public void initialize() {
        // Texto por defecto si aún no hay usuario
        lblSubtitle.setText("Gestión de talleres · JavaFX + Clean Architecture");
    }

    public void setCurrentUser(User user) {
        if (user != null) {
            lblSubtitle.setText("Bienvenido, " + user.getNombre());
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
}
