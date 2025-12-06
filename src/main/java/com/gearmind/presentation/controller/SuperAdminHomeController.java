package com.gearmind.presentation.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class SuperAdminHomeController {

    @FXML
    private Button btnEmpresas;

    @FXML
    private Button btnUsuarios;

    @FXML
    private Button btnVolver;

    @FXML
    private void initialize() {
        btnEmpresas.setOnAction(e -> openEmpresas());
        btnUsuarios.setOnAction(e -> openUsuariosEmpresa());
        btnVolver.setOnAction(e -> goHome());
    }

    private void openEmpresas() {
        System.out.println("TODO: abrir gestión de empresas (super admin)");
    }

    private void openUsuariosEmpresa() {
        System.out.println("TODO: abrir gestión de usuarios de empresa (super admin)");
    }

    private void goHome() {
        try {
            Stage stage = (Stage) btnVolver.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/HomeView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());

            stage.setTitle("GearMind — Inicio");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
