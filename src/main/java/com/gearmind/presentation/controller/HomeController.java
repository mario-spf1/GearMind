package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.List;

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
    private HBox userBox;
    @FXML
    private Label lblUsuarioActual;
    @FXML
    private Button btnUserMenu;
    @FXML
    private Button btnNavDashboard;
    @FXML
    private Button btnNavCitas;
    @FXML
    private Button btnNavClientes;
    @FXML
    private Button btnNavVehiculos;
    @FXML
    private Button btnNavProductos;
    @FXML
    private Button btnNavUsuarios;
    @FXML
    private Button btnNavEmpresas;

    private javafx.scene.Node savedSidebar;
    private ContextMenu userMenu;

    @FXML
    public void initialize() {
        savedSidebar = sidebar;
        setupFromAuthContext();
        initUserMenu();

        if (userBox != null) {
            userBox.setOnMouseClicked(e -> showUserMenu());
        }
        if (lblUsuarioActual != null) {
            lblUsuarioActual.setOnMouseClicked(e -> showUserMenu());
        }

        loadView("/view/DashboardView.fxml");
        setActiveNavButton(btnNavDashboard);
    }

    private void setupFromAuthContext() {
        if (!AuthContext.isLoggedIn()) {
            if (lblUsuarioActual != null) {
                lblUsuarioActual.setText("Invitado");
            }
            applyRoleToSidebar(null);
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

        applyRoleToSidebar(role);
    }

    private void applyRoleToSidebar(UserRole role) {
        setAllSidebarButtonsVisible(true);

        if (role == null) {
            hideButton(btnNavEmpresas);
            hideButton(btnNavUsuarios);
            return;
        }

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
        for (Button b : List.of(btnNavDashboard, btnNavCitas, btnNavClientes, btnNavVehiculos, btnNavProductos, btnNavUsuarios, btnNavEmpresas)) {
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

    private void initUserMenu() {
        MenuItem miManage = new MenuItem("Gestionar cuenta");
        miManage.setOnAction(e -> onManageAccount());
        MenuItem miLogout = new MenuItem("Cerrar sesión");
        miLogout.setOnAction(e -> onLogout());
        userMenu = new ContextMenu(miManage, new SeparatorMenuItem(), miLogout);
        userMenu.setAutoHide(true);
    }

    @FXML
    private void onUserMenu() {
        showUserMenu();
    }

    private void showUserMenu() {
        if (userMenu == null || btnUserMenu == null) {
            return;
        }

        if (userMenu.isShowing()) {
            userMenu.hide();
            return;
        }

        userMenu.show(btnUserMenu, Side.BOTTOM, 0, 6);
    }

    private void onManageAccount() {
        try {
            User current = AuthContext.getCurrentUser();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UsuarioFormView.fxml"));
            Parent rootForm = loader.load();
            UsuarioFormController controller = loader.getController();
            controller.initForSelfEdit(current);
            Stage stage = new Stage();
            stage.setTitle("Mi cuenta");
            stage.initOwner(root.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.setResizable(false);
            Scene scene = new Scene(rootForm);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            if (controller.isSaved()) {
                var repo = new com.gearmind.infrastructure.auth.MySqlUserRepository();
                repo.findById(current.getId()).ifPresent(updated -> {
                    String empresaNombre = SessionManager.getInstance().getCurrentEmpresaNombre();
                    SessionManager.getInstance().startSession(updated, empresaNombre);
                    setupFromAuthContext();
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir 'Mi cuenta': " + ex.getMessage()).showAndWait();
        }
    }

    private void onLogout() {
        try {
            SessionManager.getInstance().clearSession();

            URL fxml = getClass().getResource("/view/LoginView.fxml");
            if (fxml == null) {
                throw new IOException("No se encuentra /view/LoginView.fxml en el classpath");
            }

            FXMLLoader loader = new FXMLLoader(fxml);
            Parent loginRoot = loader.load();
            Stage stage = (Stage) root.getScene().getWindow();
            double width = stage.getScene() != null ? stage.getScene().getWidth() : 1024;
            double height = stage.getScene() != null ? stage.getScene().getHeight() : 720;
            Scene scene = new Scene(loginRoot, width, height);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setTitle("GearMind — Acceso");
            stage.setScene(scene);
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Cerrar sesión");
            a.setHeaderText("No se pudo volver al login");
            a.setContentText(ex.getMessage());
            a.showAndWait();
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
        List<Button> buttons = List.of(btnNavDashboard, btnNavCitas, btnNavClientes, btnNavVehiculos, btnNavProductos, btnNavUsuarios, btnNavEmpresas);

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
    private void onNavDashboard() {
        loadView("/view/DashboardView.fxml");
        setActiveNavButton(btnNavDashboard);
    }

    @FXML
    private void onNavCitas() {
        loadView("/view/CitasView.fxml");
        setActiveNavButton(btnNavCitas);
    }

    @FXML
    private void onNavClientes() {
        loadView("/view/ClientesView.fxml");
        setActiveNavButton(btnNavClientes);
    }

    @FXML
    private void onNavVehiculos() {
        loadView("/view/VehiculosView.fxml");
        setActiveNavButton(btnNavVehiculos);
    }

    @FXML
    private void onNavProductos() {
        loadView("/view/ProductosView.fxml");
        setActiveNavButton(btnNavProductos);
    }

    @FXML
    private void onNavUsuarios() {
        loadView("/view/UsuariosView.fxml");
        setActiveNavButton(btnNavUsuarios);
    }

    @FXML
    private void onNavEmpresas() {
        loadView("/view/EmpresasView.fxml");
        setActiveNavButton(btnNavEmpresas);
    }
}
