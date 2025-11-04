package com.gearmind.presentation.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController {
    @FXML private Label lblSubtitle;

    @FXML
    public void initialize() {
        lblSubtitle.setText("Gestión de talleres · JavaFX + Clean Architecture");
    }

    @FXML
    public void onNewProject() {
        // Aquí iría la navegación a “Nuevo proyecto / flujo inicial”
        System.out.println("Nuevo proyecto (placeholder)");
    }

    @FXML
    public void onOpen() {
        // Aquí iría la navegación al login o al dashboard
        System.out.println("Abrir (placeholder)");
    }
}
