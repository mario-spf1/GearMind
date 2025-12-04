package com.gearmind.presentation.controller;

import com.gearmind.application.common.SessionManager;
import com.gearmind.application.user.DeactivateUserUseCase;
import com.gearmind.application.user.ListUsersUseCase;
import com.gearmind.domain.user.User;
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

    @FXML private TableView<User> tblUsuarios;
    @FXML private TableColumn<User, String> colNombre;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRol;
    @FXML private TableColumn<User, String> colEstado;
    @FXML private TableColumn<User, User> colAcciones;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Integer> cmbPageSize;
    @FXML private Label lblResumen;

    private final ListUsersUseCase listUsersUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;

    private final ObservableList<User> masterData = FXCollections.observableArrayList();

    public UsuariosController() {
        MySqlUserRepository repo = new MySqlUserRepository();
        this.listUsersUseCase = new ListUsersUseCase(repo);
        this.deactivateUserUseCase = new DeactivateUserUseCase(repo);
    }

    @FXML
    private void initialize() {
        tblUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRol.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getRol().name())
        );
        colEstado.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().isActivo() ? "Activo" : "Inactivo")
        );

        setupAccionesColumn();

        cmbPageSize.setItems(FXCollections.observableArrayList(5, 10, 25, 50));
        cmbPageSize.getSelectionModel().select(Integer.valueOf(10));
        cmbPageSize.valueProperty().addListener((obs, o, n) -> refreshTable());
        txtBuscar.textProperty().addListener((obs, o, n) -> refreshTable());

        setupRowDoubleClick();
        loadUsuariosFromDb();
    }

    private void setupAccionesColumn() {
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {

            private final Button btnEditar = new Button("Editar");
            private final Button btnDesactivar = new Button("Desactivar");
            private final HBox box = new HBox(5, btnEditar, btnDesactivar);

            {
                btnEditar.getStyleClass().add("tfx-btn-primary");
                btnDesactivar.getStyleClass().add("tfx-btn-ghost");

                btnEditar.setOnAction(e -> {
                    User u = getItem();
                    if (u != null) {
                        openUsuarioForm(u);
                    }
                });

                btnDesactivar.setOnAction(e -> {
                    User u = getItem();
                    if (u != null) {
                        deactivateUser(u);
                    }
                });
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
        colAcciones.setSortable(false);
    }

    private void loadUsuariosFromDb() {
        long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
        List<User> usuarios = listUsersUseCase.listByEmpresa(empresaId);
        masterData.setAll(usuarios);
        refreshTable();
    }

    private void refreshTable() {
        String filtro = txtBuscar.getText() == null
                ? ""
                : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);

        int limit = Optional.ofNullable(cmbPageSize.getValue())
                .orElse(Integer.MAX_VALUE);

        List<User> filtered = masterData.stream()
                .filter(u -> matchesFilter(u, filtro))
                .sorted(Comparator.comparing(User::getNombre,
                        String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        int total = filtered.size();
        List<User> page = filtered.stream()
                .limit(limit)
                .collect(Collectors.toList());

        tblUsuarios.setItems(FXCollections.observableArrayList(page));
        lblResumen.setText("Mostrando " + page.size() + " de " + total + " usuarios");
    }

    private boolean matchesFilter(User u, String filtro) {
        if (filtro.isEmpty()) {
            return true;
        }
        String nombre = safe(u.getNombre());
        String email = safe(u.getEmail());
        String rol = u.getRol() != null ? u.getRol().name().toLowerCase(Locale.ROOT) : "";
        String estado = u.isActivo() ? "activo" : "inactivo";
        return nombre.contains(filtro)
                || email.contains(filtro)
                || rol.contains(filtro)
                || estado.contains(filtro);
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
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

    private void openUsuarioForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/UsuarioFormView.fxml")
            );
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

    private void deactivateUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Desactivar usuario");
        alert.setHeaderText("¿Desactivar usuario?");
        alert.setContentText("El usuario \"" + user.getNombre()
                + "\" dejará de poder acceder a la aplicación.");

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
                deactivateUserUseCase.deactivate(user.getId(), empresaId);
                loadUsuariosFromDb();
            }
        });
    }

    @FXML
    private void onNuevoUsuario() {
        openUsuarioForm(null);
    }

    @FXML
    private void onRefrescar() {
        loadUsuariosFromDb();
    }

    @FXML
    private void onVolverHome() {
        try {
            Stage stage = (Stage) tblUsuarios.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/HomeView.fxml")
            );
            Parent root = loader.load();

            Scene scene = new Scene(
                    root,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight()
            );

            scene.getStylesheets().add(
                    getClass().getResource("/styles/theme.css").toExternalForm()
            );
            scene.getStylesheets().add(
                    getClass().getResource("/styles/components.css").toExternalForm()
            );

            stage.setTitle("GearMind — Inicio");
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
