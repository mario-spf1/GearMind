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
import java.util.Optional;
import java.util.stream.Collectors;

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

    private final ListUsersUseCase listUsersUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    private final SaveUserUseCase saveUserUseCase;

    private final ObservableList<User> masterData = FXCollections.observableArrayList();

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
        colRol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getRol().name()));
        colEstado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        setupAccionesColumn();
        setupRowDoubleClick();
        cmbPageSize.setItems(FXCollections.observableArrayList(5, 10, 25, 50));
        cmbPageSize.getSelectionModel().select(Integer.valueOf(10));
        cmbPageSize.valueProperty().addListener((obs, o, n) -> refreshTable());
        txtBuscar.textProperty().addListener((obs, o, n) -> refreshTable());
        loadUsuariosFromDb();
    }

    private void setupAccionesColumn() {
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {

            private final Button btnEditar = new Button("Editar");
            private final Button btnToggle = new Button();
            private final HBox box = new HBox(6, btnEditar, btnToggle);

            {
                btnEditar.getStyleClass().add("tfx-btn-ghost");
                btnToggle.getStyleClass().add("tfx-btn-ghost");
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
                    btnToggle.setText(user.isActivo() ? "Desactivar" : "Activar");
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

    /**
     * Carga de BD → masterData → refreshTable (igual que en clientes).
     */
    private void loadUsuariosFromDb() {
        long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
        List<User> usuarios = listUsersUseCase.listByEmpresa(empresaId);
        masterData.setAll(usuarios);
        refreshTable();
    }

    /**
     * Aplica filtro + "paginación" sobre masterData y actualiza la tabla.
     */
    private void refreshTable() {
        String filtro = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        int limit = Optional.ofNullable(cmbPageSize.getValue()).orElse(Integer.MAX_VALUE);
        List<User> filtered = masterData.stream().filter(u -> matchesFilter(u, filtro)).sorted(Comparator.comparing(User::getNombre, String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList());
        int total = filtered.size();
        List<User> visible = filtered.subList(0, Math.min(limit, total));
        tblUsuarios.setItems(FXCollections.observableArrayList(visible));
        lblResumen.setText("Mostrando " + visible.size() + " de " + total + " usuarios");
    }

    private boolean matchesFilter(User u, String filtro) {
        if (filtro.isEmpty()) {
            return true;
        }
        String nombre = safe(u.getNombre());
        String email = safe(u.getEmail());
        String rol = u.getRol() != null ? u.getRol().name().toLowerCase(Locale.ROOT) : "";
        String estado = u.isActivo() ? "activo" : "inactivo";
        return nombre.contains(filtro) || email.contains(filtro) || rol.contains(filtro) || estado.contains(filtro);
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    /**
     * Si está activo → desactiva. Si está inactivo → activa.
     */
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

                SaveUserRequest request = new SaveUserRequest(user.getId(), empresaId, user.getNombre(), user.getEmail(), "", user.getRol(), true);
                saveUserUseCase.save(request);
                loadUsuariosFromDb();
            }
        });
    }

    /**
     * Abre el form de usuario (nuevo o edición). Si se guarda → recarga BD.
     */
    private void openUsuarioForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UsuarioFormView.fxml"));
            Parent root = loader.load();
            UsuarioFormController controller = loader.getController();
            if (user == null) {
                controller.initForNew();
            } else {
                controller.initForEdit(user);
            }

            Stage dialog = new Stage();
            dialog.initOwner(tblUsuarios.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle(user == null ? "Nuevo usuario" : "Editar usuario");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            dialog.setScene(scene);
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
