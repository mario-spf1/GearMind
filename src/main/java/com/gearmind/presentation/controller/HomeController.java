package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.List;

public class HomeController {

    @FXML
    private BorderPane root;
    @FXML
    private VBox sidebar;
    @FXML
    private StackPane contentPane;
    @FXML
    private Label lblUsuarioActual;
    @FXML
    private Label lblAppTitle;
    @FXML
    private Button btnNavDashboard;
    @FXML
    private Button btnNavCitas;
    @FXML
    private Button btnNavClientes;
    @FXML
    private Button btnNavVehiculos;
    @FXML
    private Button btnNavUsuarios;
    @FXML
    private Button btnNavEmpresas;

    private Node savedSidebar;

    @FXML
    public void initialize() {
        savedSidebar = sidebar;
        setupFromAuthContext();
        loadView("/view/DashboardView.fxml");
        setActiveNavButton(btnNavDashboard);
    }

    private void setupFromAuthContext() {
        if (!AuthContext.isLoggedIn()) {
            if (lblUsuarioActual != null) {
                lblUsuarioActual.setText("Invitado");
            }
            return;
        }

        User user = AuthContext.getCurrentUser();
        UserRole role = AuthContext.getRole();

        if (lblUsuarioActual != null && user != null) {
            String rolTexto = switch (role) {
                case SUPER_ADMIN ->
                    "Super admin";
                case ADMIN ->
                    "Admin";
                case EMPLEADO ->
                    "Empleado";
            };
            lblUsuarioActual.setText(user.getNombre() + " (" + rolTexto + ")");
        }

        if (lblAppTitle != null) {
            String empresaNombre = AuthContext.getEmpresaNombre();
            if (empresaNombre != null && !empresaNombre.isBlank()) {
                lblAppTitle.setText("GearMind · " + empresaNombre);
            } else {
                lblAppTitle.setText("GearMind");
            }
        }

        applyRoleToSidebar(role);
    }

    private void applyRoleToSidebar(UserRole role) {
        setAllSidebarButtonsVisible(true);

        if (role == UserRole.SUPER_ADMIN) {
            return;
        }

        if (role == UserRole.ADMIN) {
            hideButton(btnNavEmpresas);
            return;
        }

        if (role == UserRole.EMPLEADO) {
            hideButton(btnNavEmpresas);
            hideButton(btnNavUsuarios);
        }
    }

    private void setAllSidebarButtonsVisible(boolean visible) {
        for (Button b : List.of(btnNavDashboard, btnNavCitas, btnNavClientes, btnNavVehiculos, btnNavUsuarios, btnNavEmpresas)) {
            if (b != null) {
                b.setVisible(visible);
                b.setManaged(visible);
            }
        }
    }

    private void hideButton(Button b) {
        if (b != null) {
            b.setVisible(false);
            b.setManaged(false);
        }
    }

    private void loadView(String fxmlPath) {
        try {
            var url = getClass().getResource(fxmlPath);
            if (url == null) {
                throw new IllegalStateException("No se ha encontrado la vista: " + fxmlPath);
            }
            Parent view = FXMLLoader.load(url);
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveNavButton(Button activeButton) {
        List<Button> buttons = List.of(btnNavDashboard, btnNavCitas, btnNavClientes, btnNavUsuarios, btnNavEmpresas);

        for (Button b : buttons) {
            if (b != null) {
                b.getStyleClass().remove("tfx-nav-active");
            }
        }

        if (activeButton != null && !activeButton.getStyleClass().contains("tfx-nav-active")) {
            activeButton.getStyleClass().add("tfx-nav-active");
        }
    }

    @FXML
    private void onToggleSidebar() {
        if (root.getLeft() == null) {
            root.setLeft(savedSidebar);
        } else {
            root.setLeft(null);
        }
    }

    @FXML
    private void onGoPerfilUsuario() {
        System.out.println("Ir a mi perfil (placeholder)");
    }

    @FXML
    private void onGoDashboard() {
        loadView("/view/DashboardView.fxml");
        setActiveNavButton(btnNavDashboard);
    }

    @FXML
    private void onGoCitas() {
        loadView("/view/CitasView.fxml");
        setActiveNavButton(btnNavCitas);
    }

    @FXML
    private void onGoClientes() {
        loadView("/view/ClientesView.fxml");
        setActiveNavButton(btnNavClientes);
    }

    @FXML
    private void onGoVehiculos() {
        loadView("/view/VehiculosView.fxml");
        setActiveNavButton(btnNavVehiculos);
    }

    @FXML
    private void onGoUsuarios() {
        loadView("/view/UsuariosView.fxml");
        setActiveNavButton(btnNavUsuarios);
    }

    @FXML
    private void onGoEmpresas() {
        loadView("/view/EmpresasView.fxml");
        setActiveNavButton(btnNavEmpresas);
    }
}
