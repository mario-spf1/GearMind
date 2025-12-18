package com.gearmind.presentation.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class HomeController {

    @FXML
    private BorderPane root;
    @FXML
    private VBox sidebar;
    @FXML
    private StackPane contentPane;
    @FXML
    private Button btnToggleSidebar;
    @FXML
    private Label lblUsuarioActual;
    @FXML
    private Button btnNavDashboard;
    @FXML
    private Button btnNavClientes;
    @FXML
    private Button btnNavVehiculos;
    @FXML
    private Button btnNavCitas;
    @FXML
    private Button btnNavUsuarios;
    @FXML
    private Button btnNavEmpresas;

    private VBox savedSidebar;
    private static final String NAV_ACTIVE_CLASS = "tfx-nav-active";

    @FXML
    public void initialize() {
        savedSidebar = sidebar;

        if (lblUsuarioActual != null) {
            lblUsuarioActual.setText("Mario Rodríguez  |  Mi cuenta");
        }

        loadView("/view/DashboardView.fxml");
        setActive(btnNavDashboard);
    }

    @FXML
    private void onToggleSidebar() {
        if (root == null) {
            return;
        }

        if (root.getLeft() == null) {
            root.setLeft(savedSidebar);
        } else {
            root.setLeft(null);
        }
    }

    @FXML
    private void onNavDashboard() {
        loadView("/view/DashboardView.fxml");
        setActive(btnNavDashboard);
    }

    @FXML
    private void onNavClientes() {
        loadView("/view/ClientesView.fxml");
        setActive(btnNavClientes);
    }

    @FXML
    private void onNavVehiculos() {
        loadView("/view/VehiculosView.fxml");
        setActive(btnNavVehiculos);
    }

    @FXML
    private void onNavCitas() {
        loadView("/view/CitasView.fxml");
        setActive(btnNavCitas);
    }

    @FXML
    private void onNavUsuarios() {
        loadView("/view/UsuariosView.fxml");
        setActive(btnNavUsuarios);
    }

    @FXML
    private void onNavEmpresas() {
        loadView("/view/EmpresasView.fxml");
        setActive(btnNavEmpresas);
    }

    private void loadView(String fxmlPath) {
        if (contentPane == null) {
            return;
        }

        try {
            Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(view);
        } catch (Exception ex) {
            Label placeholder = new Label("Vista no disponible: " + fxmlPath);
            placeholder.getStyleClass().add("tfx-warn");
            contentPane.getChildren().setAll(placeholder);
        }
    }

    /**
     * Marca el botón activo según tu CSS: ".tfx-nav-active". Limpia primero esa
     * clase de todos los botones del sidebar.
     */
    private void setActive(Button active) {
        if (sidebar == null) {
            return;
        }

        sidebar.lookupAll(".button").forEach(n -> n.getStyleClass().remove(NAV_ACTIVE_CLASS));
        if (active != null && !active.getStyleClass().contains(NAV_ACTIVE_CLASS)) {
            active.getStyleClass().add(NAV_ACTIVE_CLASS);
        }
    }
}
