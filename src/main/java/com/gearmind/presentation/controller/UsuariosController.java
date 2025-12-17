package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.user.DeactivateUserUseCase;
import com.gearmind.application.user.ListUsersUseCase;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.database.DataSourceFactory;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import com.gearmind.presentation.table.SmartTable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class UsuariosController {

    @FXML private TableView<User> tblUsuarios;

    @FXML private TableColumn<User, String> colEmpresa;  // solo superadmin
    @FXML private TableColumn<User, String> colNombre;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRol;
    @FXML private TableColumn<User, String> colEstado;
    @FXML private TableColumn<User, User> colAcciones;

    @FXML private Button btnNuevo;

    @FXML private ComboBox<Integer> cmbPageSize;
    @FXML private Label lblResumen;

    // filtros
    @FXML private HBox boxFilterEmpresa;                 // contenedor para ocultar
    @FXML private ComboBox<String> filterEmpresaCombo;   // solo superadmin

    @FXML private TextField filterNombreField;
    @FXML private TextField filterEmailField;

    @FXML private ComboBox<String> filterRolCombo;
    @FXML private ComboBox<String> filterEstadoCombo;

    private final ObservableList<User> masterData = FXCollections.observableArrayList();
    private SmartTable<User> smartTable;

    private final MySqlUserRepository repo = new MySqlUserRepository();
    private final ListUsersUseCase listUsersUseCase = new ListUsersUseCase(repo);
    private final DeactivateUserUseCase deactivateUserUseCase = new DeactivateUserUseCase(repo);

    private final DataSource dataSource = DataSourceFactory.getDataSource();

    private final Map<Long, String> empresaNombreById = new HashMap<>();
    private final List<Long> empresaIdsOrdenadas = new ArrayList<>();

    @FXML
    private void initialize() {

        // ✅ Acceso: SOLO ADMIN y SUPER_ADMIN
        if (AuthContext.isEmpleado()) {
            if (btnNuevo != null) btnNuevo.setDisable(true);
            if (cmbPageSize != null) cmbPageSize.setDisable(true);

            tblUsuarios.setDisable(true);
            tblUsuarios.setPlaceholder(new Label("Acceso restringido: solo Admin/SuperAdmin puede gestionar usuarios."));
            if (lblResumen != null) lblResumen.setText("Acceso restringido (Admin/SuperAdmin).");
            return;
        }

        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        tblUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblUsuarios.setPlaceholder(new Label("No hay usuarios que mostrar."));

        // ===== Empresa (solo superadmin) =====
        if (!isSuperAdmin) {
            if (colEmpresa != null) colEmpresa.setVisible(false);
            if (boxFilterEmpresa != null) {
                boxFilterEmpresa.setVisible(false);
                boxFilterEmpresa.setManaged(false);
            }
        } else {
            cargarEmpresasCache(); // para columna y filtro
            if (colEmpresa != null) {
                colEmpresa.setCellValueFactory(u ->
                        new SimpleStringProperty(empresaNombreById.getOrDefault(u.getValue().getEmpresaId(), ""))
                );
            }
        }

        // ===== Columnas =====
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRol.setCellValueFactory(u -> new SimpleStringProperty(u.getValue().getRol().name()));

        // Estado badge
        colEstado.setCellValueFactory(u ->
                new ReadOnlyObjectWrapper<>(u.getValue().isActivo() ? "Activo" : "Inactivo")
        );
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-badge-success", "tfx-badge-danger");

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().add("tfx-badge");
                    getStyleClass().add("Activo".equalsIgnoreCase(item) ? "tfx-badge-success" : "tfx-badge-danger");
                }
            }
        });

        setupAccionesColumn();
        setupRowDoubleClick();

        // ===== Page size =====
        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(5, 15, 25, 0));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(15));

            var converter = new javafx.util.StringConverter<Integer>() {
                @Override public String toString(Integer value) {
                    if (value == null) return "";
                    return value == 0 ? "Todos" : String.valueOf(value);
                }
                @Override public Integer fromString(String s) {
                    if (s == null) return 15;
                    s = s.trim();
                    return "Todos".equalsIgnoreCase(s) ? 0 : Integer.valueOf(s);
                }
            };

            cmbPageSize.setConverter(converter);
            cmbPageSize.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : (item == 0 ? "Todos" : String.valueOf(item)));
                }
            });
            cmbPageSize.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : (item == 0 ? "Todos" : String.valueOf(item)));
                }
            });
        }

        // ===== SmartTable =====
        smartTable = new SmartTable<>(tblUsuarios, masterData, null, cmbPageSize, lblResumen, "usuarios", null);

        tblUsuarios.setFixedCellSize(28);
        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblUsuarios.getFixedCellSize() + 2;
            tblUsuarios.setPrefHeight(tableHeight);
            tblUsuarios.setMinHeight(Region.USE_PREF_SIZE);
        });

        // ===== Filtros =====
        smartTable.addColumnFilter(filterNombreField, (u, text) -> safe(u.getNombre()).contains(text));
        smartTable.addColumnFilter(filterEmailField, (u, text) -> safe(u.getEmail()).contains(text));

        if (filterRolCombo != null) {
            filterRolCombo.setItems(FXCollections.observableArrayList(
                    "Todos",
                    UserRole.SUPER_ADMIN.name(),
                    UserRole.ADMIN.name(),
                    UserRole.EMPLEADO.name()
            ));
            filterRolCombo.getSelectionModel().select("Todos");

            smartTable.addColumnFilter(filterRolCombo, (u, selected) -> {
                if (selected == null || "Todos".equalsIgnoreCase(selected)) return true;
                return u.getRol().name().equalsIgnoreCase(selected);
            });
        }

        if (filterEstadoCombo != null) {
            filterEstadoCombo.setItems(FXCollections.observableArrayList("Todos", "Activo", "Inactivo"));
            filterEstadoCombo.getSelectionModel().select("Todos");

            smartTable.addColumnFilter(filterEstadoCombo, (u, selected) -> {
                if (selected == null || "Todos".equalsIgnoreCase(selected)) return true;
                return "Activo".equalsIgnoreCase(selected) ? u.isActivo() : !u.isActivo();
            });
        }

        if (isSuperAdmin && filterEmpresaCombo != null) {
            List<String> nombres = empresaIdsOrdenadas.stream()
                    .map(id -> empresaNombreById.getOrDefault(id, ""))
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());

            List<String> items = new ArrayList<>();
            items.add("Todas");
            items.addAll(nombres);

            filterEmpresaCombo.setItems(FXCollections.observableArrayList(items));
            filterEmpresaCombo.getSelectionModel().select("Todas");

            smartTable.addColumnFilter(filterEmpresaCombo, (u, selected) -> {
                if (selected == null || "Todas".equalsIgnoreCase(selected)) return true;
                String empName = empresaNombreById.getOrDefault(u.getEmpresaId(), "");
                return empName.equalsIgnoreCase(selected);
            });

            filterEmpresaCombo.valueProperty().addListener((obs, o, n) -> cargarUsuarios()); // refresca lista
        }

        cargarUsuarios();
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
                    if (u != null) openUsuarioForm(u);
                });

                btnToggle.setOnAction(e -> {
                    User u = getItem();
                    if (u != null) toggleActivo(u);
                });
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    return;
                }

                btnToggle.getStyleClass().removeAll("tfx-icon-btn-danger", "tfx-icon-btn-success");

                if (user.isActivo()) {
                    btnToggle.setText("Desactivar");
                    btnToggle.getStyleClass().add("tfx-icon-btn-danger");
                } else {
                    btnToggle.setText("Activar");
                    btnToggle.getStyleClass().add("tfx-icon-btn-success");
                }

                setGraphic(box);
            }
        });

        colAcciones.setSortable(false);
    }

    private void setupRowDoubleClick() {
        tblUsuarios.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openUsuarioForm(row.getItem());
                }
            });
            return row;
        });
    }

    @FXML
    private void onNuevoUsuario() {
        if (AuthContext.isEmpleado()) return;
        openUsuarioForm(null);
    }

    @FXML
    private void onRefrescar() {
        if (AuthContext.isEmpleado()) return;
        cargarUsuarios();
    }

    @FXML
    private void onLimpiarFiltros() {
        if (filterNombreField != null) filterNombreField.clear();
        if (filterEmailField != null) filterEmailField.clear();
        if (filterRolCombo != null) filterRolCombo.getSelectionModel().select("Todos");
        if (filterEstadoCombo != null) filterEstadoCombo.getSelectionModel().select("Todos");
        if (filterEmpresaCombo != null) filterEmpresaCombo.getSelectionModel().select("Todas");
        smartTable.refresh();
    }

    private void toggleActivo(User user) {
        try {
            long empresaId = AuthContext.isSuperAdmin()
                    ? user.getEmpresaId()
                    : SessionManager.getInstance().getCurrentEmpresaId();

            // DeactivateUseCase solo desactiva (según tu clase).
            // Si quieres activar, se hace editando usuario y marcándolo activo.
            if (user.isActivo()) {
                deactivateUserUseCase.deactivate(user.getId(), empresaId);
            } else {
                // activar => abrir editar y marcar activo (simple y consistente con tu capa application)
                openUsuarioForm(user);
                return;
            }

            cargarUsuarios();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo cambiar el estado: " + ex.getMessage()).showAndWait();
        }
    }

    private void openUsuarioForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UsuarioFormView.fxml"));
            Parent root = loader.load();

            UsuarioFormController controller = loader.getController();
            if (user == null) controller.initForNew();
            else controller.initForEdit(user);

            Stage stage = new Stage();
            stage.setTitle(user == null ? "Nuevo usuario" : "Editar usuario");
            stage.initOwner(tblUsuarios.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setScene(scene);

            stage.showAndWait();

            if (controller.isSaved()) {
                cargarUsuarios();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario: " + ex.getMessage()).showAndWait();
        }
    }

    private void cargarUsuarios() {
        masterData.clear();

        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        if (!isSuperAdmin) {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            masterData.setAll(listUsersUseCase.listByEmpresa(empresaId));
            smartTable.refresh();
            return;
        }

        // superadmin: opcional filtrar por empresa seleccionada
        String selectedEmpresa = (filterEmpresaCombo != null) ? filterEmpresaCombo.getValue() : "Todas";

        if (selectedEmpresa == null || "Todas".equalsIgnoreCase(selectedEmpresa)) {
            for (Long empId : empresaIdsOrdenadas) {
                masterData.addAll(listUsersUseCase.listByEmpresa(empId));
            }
        } else {
            // buscar id de esa empresa por nombre
            Long empId = empresaNombreById.entrySet().stream()
                    .filter(e -> e.getValue().equalsIgnoreCase(selectedEmpresa))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);

            if (empId != null) {
                masterData.addAll(listUsersUseCase.listByEmpresa(empId));
            }
        }

        smartTable.refresh();
    }

    private void cargarEmpresasCache() {
        empresaNombreById.clear();
        empresaIdsOrdenadas.clear();

        String sql = "SELECT id, nombre FROM empresa ORDER BY nombre ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("id");
                String nombre = rs.getString("nombre");
                empresaNombreById.put(id, nombre);
                empresaIdsOrdenadas.add(id);
            }
        } catch (Exception ex) {
            throw new RuntimeException("No se pudieron cargar las empresas", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
