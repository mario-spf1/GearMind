package com.gearmind.presentation.controller;

import com.gearmind.application.common.SessionManager;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {

    @FXML
    private Label lblSubtitle;

    @FXML
    private VBox cardUsuarios;

    @FXML
    private VBox cardInventario;

    @FXML
    private VBox cardConfiguracion;

    @FXML
    private VBox cardInformes;

    @FXML
    private VBox cardEmpresas;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();

        if (user != null) {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            lblSubtitle.setText("Bienvenido, " + user.getNombre() + " · Empresa " + empresaId + " · Rol " + user.getRol());

            boolean isAdmin = user.getRol() == UserRole.ADMIN;
            boolean isSuperAdmin = user.getRol() == UserRole.SUPER_ADMIN;
            configureAdminCards(isAdmin || isSuperAdmin);
            configureEmpresasCard(isSuperAdmin);
        } else {
            lblSubtitle.setText("Gestión de talleres · GearMind");
            configureAdminCards(false);
            configureEmpresasCard(false);
        }
    }

    private void configureAdminCards(boolean visible) {
        if (cardUsuarios != null) {
            cardUsuarios.setVisible(visible);
            cardUsuarios.setManaged(visible);
        }
        if (cardInventario != null) {
            cardInventario.setVisible(visible);
            cardInventario.setManaged(visible);
        }
        if (cardConfiguracion != null) {
            cardConfiguracion.setVisible(visible);
            cardConfiguracion.setManaged(visible);
        }
        if (cardInformes != null) {
            cardInformes.setVisible(visible);
            cardInformes.setManaged(visible);
        }
    }

    private void configureEmpresasCard(boolean visible) {
        if (cardEmpresas != null) {
            cardEmpresas.setVisible(visible);
            cardEmpresas.setManaged(visible);
        }
    }

    @FXML
    public void onOpen() {
        System.out.println("Abrir (placeholder)");
    }

    @FXML
    public void onNewProject() {
        System.out.println("Nuevo (placeholder)");
    }

    @FXML
    private void onGoToClientes() {
        try {
            Stage stage = (Stage) lblSubtitle.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ClientesView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());

            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());

            stage.setTitle("GearMind — Clientes");
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onGoToCitas() {
        System.out.println("Ir a Citas (módulo)");
    }

    @FXML
    private void onGoToReparaciones() {
        System.out.println("Ir a Reparaciones (módulo)");
    }

    @FXML
    private void onGoToPresupuestos() {
        System.out.println("Ir a Presupuestos (módulo)");
    }

    @FXML
    private void onGoToFacturacion() {
        System.out.println("Ir a Facturación (módulo)");
    }

    @FXML
    private void onGoToComunicaciones() {
        System.out.println("Ir a Comunicaciones (módulo)");
    }

    @FXML
    private void onGoToUsuarios() {
        try {
            Stage stage = (Stage) lblSubtitle.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UsuariosView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());

            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());

            stage.setTitle("GearMind — Usuarios & Roles");
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onGoToInventario() {
        System.out.println("Ir a Inventario (módulo ADMIN)");
    }

    @FXML
    private void onGoToConfiguracion() {
        System.out.println("Ir a Configuración (módulo ADMIN)");
    }

    @FXML
    private void onGoToInformes() {
        System.out.println("Ir a Informes & KPIs (módulo ADMIN)");
    }

    @FXML
    private void onGoToEmpresas() {
        loadView("/view/EmpresasView.fxml", "GearMind — Gestión de empresas");
    }

    @FXML
    private void onLogout() {
        try {
            SessionManager.getInstance().clearSession();

            Stage stage = (Stage) lblSubtitle.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());

            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());

            stage.setTitle("GearMind — Acceso");
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlPath, String title) {
        try {
            Stage stage = (Stage) lblSubtitle.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());

            stage.setTitle(title);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
