package com.gearmind.presentation.controller;

import com.gearmind.application.common.SessionManager;
import com.gearmind.domain.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController {

    @FXML
    private Label lblSubtitle;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();

        if (user != null) {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            lblSubtitle.setText("Bienvenido, " + user.getNombre() + " · Empresa " + empresaId);
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
}
