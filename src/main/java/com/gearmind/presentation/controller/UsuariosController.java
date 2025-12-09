package com.gearmind.presentation.controller;

import com.gearmind.application.common.SessionManager;
import com.gearmind.application.user.DeactivateUserUseCase;
import com.gearmind.application.user.ListUsersUseCase;
import com.gearmind.application.user.SaveUserRequest;
import com.gearmind.application.user.SaveUserUseCase;
import com.gearmind.domain.security.PasswordHasher;
import com.gearmind.domain.user.User;
import com.gearmind.infrastructure.auth.BCryptPasswordHasher;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import com.gearmind.presentation.table.SmartTable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class UsuariosController {

    @FXML
    private TableView<User> tblUsuarios;
    @FXML
    private TableColumn<User, String> colNombre;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colRol;
    @FXML
    private TableColumn<User, String> colEstado;
    @FXML
    private TableColumn<User, User> colAcciones;

    @FXML
    private TextField txtBuscar;
    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Label lblResumen;

    // Filtros por columna (footer, igual que clientes)
    @FXML
    private TextField filterNombreField;
    @FXML
    private TextField filterEmailField;
    @FXML
    private TextField filterRolField;
    @FXML
    private TextField filterEstadoField;

    private final ListUsersUseCase listUsersUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    private final SaveUserUseCase saveUserUseCase;

    private final ObservableList<User> masterData = FXCollections.observableArrayList();
    private SmartTable<User> smartTable;

    public UsuariosController() {
        var repo = new MySqlUserRepository();
        this.listUsersUseCase = new ListUsersUseCase(repo);
        this.deactivateUserUseCase = new DeactivateUserUseCase(repo);
        PasswordHasher hasher = new BCryptPasswordHasher();
        this.saveUserUseCase = new SaveUserUseCase(repo, hasher);
    }

    @FXML
    private void initialize() {
        tblUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().getRol() != null ? c.getValue().getRol().name() : ""
        ));

        colEstado.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-badge-success", "tfx-badge-danger");

                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    getStyleClass().add("tfx-badge");
                    if ("Activo".equalsIgnoreCase(item)) {
                        getStyleClass().add("tfx-badge-success");
                    } else {
                        getStyleClass().add("tfx-badge-danger");
                    }
                }
            }
        });

        setupAccionesColumn();
        setupRowDoubleClick();

        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(5, 10, 25, 50));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(10));
        }

        // SmartTable: igual que Clientes
        smartTable = new SmartTable<>(
                tblUsuarios,
                masterData,
                txtBuscar,
                cmbPageSize,
                lblResumen,
                "usuarios",
                this::matchesGlobalFilter
        );

        // Altura dinámica de la tabla (opcional, igual que clientes)
        tblUsuarios.setFixedCellSize(28);
        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblUsuarios.getFixedCellSize() + 2;
            tblUsuarios.setPrefHeight(tableHeight);
        });

        // Filtros por columna (footer)
        smartTable.addColumnFilter(filterNombreField, (u, text) -> safe(u.getNombre()).contains(text));
        smartTable.addColumnFilter(filterEmailField, (u, text) -> safe(u.getEmail()).contains(text));
        smartTable.addColumnFilter(filterRolField, (u, text) -> {
            String rol = u.getRol() != null ? u.getRol().name().toLowerCase(Locale.ROOT) : "";
            return rol.contains(text);
        });
        smartTable.addColumnFilter(filterEstadoField, (u, text) ->
                (u.isActivo() ? "activo" : "inactivo").toLowerCase(Locale.ROOT).contains(text));

        loadUsuariosFromDb();
    }

    private void setupAccionesColumn() {
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {

            private final Button btnEditar = new Button("Editar");
            private final Button btnToggle = new Button();
            private final HBox box = new HBox(8, btnEditar, btnToggle);

            {
                btnEditar.getStyleClass().add("tfx-icon-btn");
                btnToggle.getStyleClass().add("tfx-icon-btn");

                btnEditar.setTooltip(new Tooltip("Editar usuario"));
                btnToggle.setTooltip(new Tooltip("Activar/Desactivar"));

                btnEditar.setOnAction(e -> {
                    User u = getItem();
                    if (u != null) {
                        openUsuarioForm(u);
                    }
                });

                btnToggle.setOnAction(e -> {
                    User u = getItem();
                    if (u != null) {
                        toggleUserActive(u);
                    }
                });
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    btnToggle.getStyleClass().removeAll("tfx-icon-btn-danger", "tfx-icon-btn-success");

                    if (user.isActivo()) {
                        btnToggle.setText("Desactivar");
                        btnToggle.getStyleClass().add("tfx-icon-btn-danger");
                        btnToggle.setTooltip(new Tooltip("Desactivar usuario"));
                    } else {
                        btnToggle.setText("Activar");
                        btnToggle.getStyleClass().add("tfx-icon-btn-success");
                        btnToggle.setTooltip(new Tooltip("Activar usuario"));
                    }

                    setGraphic(box);
                }
            }
        });
        colAcciones.setSortable(false);
    }

    private void setupRowDoubleClick() {
        tblUsuarios.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    User u = row.getItem();
                    openUsuarioForm(u);
                }
            });
            return row;
        });
    }

    private void loadUsuariosFromDb() {
        long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
        List<User> usuarios = listUsersUseCase.listByEmpresa(empresaId);

        usuarios.sort(Comparator.comparing(User::getNombre, String.CASE_INSENSITIVE_ORDER));

        masterData.setAll(usuarios);
        smartTable.refresh();
    }

    private boolean matchesGlobalFilter(User u, String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return true;
        }
        String f = filtro.toLowerCase(Locale.ROOT);
        String nombre = safe(u.getNombre());
        String email = safe(u.getEmail());
        String rol = u.getRol() != null ? u.getRol().name().toLowerCase(Locale.ROOT) : "";
        String estado = u.isActivo() ? "activo" : "inactivo";

        return nombre.contains(f) || email.contains(f) || rol.contains(f) || estado.contains(f);
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private void toggleUserActive(User user) {
        if (user.isActivo()) {
            deactivateUser(user);
        } else {
            activateUser(user);
        }
    }

    private void deactivateUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Desactivar usuario");
        alert.setHeaderText("¿Desactivar usuario?");
        alert.setContentText("El usuario \"" + user.getNombre() + "\" dejará de poder acceder a la aplicación.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
                deactivateUserUseCase.deactivate(user.getId(), empresaId);
                loadUsuariosFromDb();
            }
        });
    }

    private void activateUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Activar usuario");
        alert.setHeaderText("¿Activar usuario?");
        alert.setContentText("El usuario \"" + user.getNombre() + "\" volverá a poder acceder a la aplicación.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                long empresaId = SessionManager.getInstance().getCurrentEmpresaId();

                SaveUserRequest request = new SaveUserRequest(
                        user.getId(),
                        empresaId,
                        user.getNombre(),
                        user.getEmail(),
                        "",           // la contraseña no se toca aquí
                        user.getRol(),
                        true
                );
                saveUserUseCase.save(request);
                loadUsuariosFromDb();
            }
        });
    }

    private void openUsuarioForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UsuarioFormView.fxml"));
            Parent root = loader.load();

            UsuarioFormController controller = loader.getController();
            controller.setUser(user);   // <- aquí usamos setUser que ahora te doy

            Stage dialog = new Stage();
            dialog.initOwner(tblUsuarios.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle(user == null ? "Nuevo usuario" : "Editar usuario");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

            if (controller.isSaved()) {
                loadUsuariosFromDb();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNuevoUsuario() {
        openUsuarioForm(null);
    }

    @FXML
    private void onRefrescar() {
        loadUsuariosFromDb();
    }
}
