package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.task.AssignTaskUseCase;
import com.gearmind.application.task.ChangeTaskStatusUseCase;
import com.gearmind.application.task.ListTasksUseCase;
import com.gearmind.application.task.SaveTaskUseCase;
import com.gearmind.application.task.SetTaskPriorityUseCase;
import com.gearmind.domain.task.Task;
import com.gearmind.domain.task.TaskPriority;
import com.gearmind.domain.task.TaskStatus;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import com.gearmind.infrastructure.task.MySqlTaskRepository;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TareasController {

    @FXML
    private TableView<Task> tblTareas;
    @FXML
    private TableColumn<Task, String> colEmpresa;
    @FXML
    private TableColumn<Task, String> colCliente;
    @FXML
    private TableColumn<Task, String> colVehiculo;
    @FXML
    private TableColumn<Task, String> colProductos;
    @FXML
    private TableColumn<Task, String> colDescripcion;
    @FXML
    private TableColumn<Task, String> colEmpleado;
    @FXML
    private TableColumn<Task, String> colEstado;
    @FXML
    private TableColumn<Task, String> colPrioridad;
    @FXML
    private TableColumn<Task, Task> colAcciones;
    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Label lblResumen;
    @FXML
    private Label lblHeaderInfo;
    @FXML
    private Button btnNuevaTarea;
    @FXML
    private TextField filterClienteField;
    @FXML
    private TextField filterVehiculoField;
    @FXML
    private TextField filterProductosField;
    @FXML
    private TextField filterEmpleadoField;
    @FXML
    private ComboBox<String> filterEstadoCombo;
    @FXML
    private ComboBox<String> filterPrioridadCombo;
    @FXML
    private ComboBox<String> filterEmpresaCombo;
    @FXML
    private HBox boxFilterEmpresa;

    private final ObservableList<Task> masterData = FXCollections.observableArrayList();
    private SmartTable<Task> smartTable;

    private final ListTasksUseCase listTasksUseCase;
    private final SaveTaskUseCase saveTaskUseCase;
    private final ChangeTaskStatusUseCase changeTaskStatusUseCase;
    private final AssignTaskUseCase assignTaskUseCase;
    private final SetTaskPriorityUseCase setTaskPriorityUseCase;

    public TareasController() {
        MySqlTaskRepository repo = new MySqlTaskRepository();
        this.listTasksUseCase = new ListTasksUseCase(repo);
        this.saveTaskUseCase = new SaveTaskUseCase(repo);
        this.changeTaskStatusUseCase = new ChangeTaskStatusUseCase(repo);
        this.assignTaskUseCase = new AssignTaskUseCase(repo);
        this.setTaskPriorityUseCase = new SetTaskPriorityUseCase(repo);
    }

    @FXML
    private void initialize() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();
        boolean isAdmin = AuthContext.isAdminOrSuperAdmin();

        tblTareas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblTareas.setPlaceholder(new Label("No hay tareas que mostrar."));

        if (!isSuperAdmin) {
            if (colEmpresa != null) {
                colEmpresa.setVisible(false);
            }
            if (boxFilterEmpresa != null) {
                boxFilterEmpresa.setVisible(false);
                boxFilterEmpresa.setManaged(false);
            }
        } else {
            if (colEmpresa != null) {
                colEmpresa.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getEmpresaNombre())));
            }
        }

        colCliente.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getClienteNombre())));
        colVehiculo.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getVehiculoMatricula())));
        colProductos.setCellValueFactory(c -> new SimpleStringProperty(trimDescripcion(c.getValue().getRepairDescripcion())));

        colDescripcion.setCellValueFactory(c -> new SimpleStringProperty(trimDescripcion(c.getValue().getTitulo())));
        colDescripcion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    Task task = (getTableRow() != null) ? getTableRow().getItem() : null;
                    if (task != null) {
                        String tooltip = safeRaw(task.getDescripcion());
                        if (tooltip.isBlank()) {
                            tooltip = safeRaw(task.getTitulo());
                        }
                        if (!tooltip.isBlank()) {
                            setTooltip(new Tooltip(tooltip));
                        } else {
                            setTooltip(null);
                        }
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });

        colEmpleado.setCellValueFactory(c -> new SimpleStringProperty(employeeLabel(c.getValue())));

        colEstado.setCellValueFactory(c -> new SimpleStringProperty(mapStatusToLabel(c.getValue().getEstado())));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-badge-success", "tfx-badge-warning", "tfx-badge-danger");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().add("tfx-badge");
                    switch (item) {
                        case "Completada" ->
                            getStyleClass().add("tfx-badge-success");
                        case "En proceso" ->
                            getStyleClass().add("tfx-badge-warning");
                        case "Cancelada" ->
                            getStyleClass().add("tfx-badge-danger");
                        case "Pendiente" ->
                            getStyleClass().add("tfx-badge-warning");
                        default -> {
                        }
                    }
                }
            }
        });

        colPrioridad.setCellValueFactory(c -> new SimpleStringProperty(mapPriorityToLabel(c.getValue().getPrioridad())));
        colPrioridad.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-badge-success", "tfx-badge-warning", "tfx-badge-danger");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().add("tfx-badge");
                    switch (item) {
                        case "Alta" ->
                            getStyleClass().add("tfx-badge-danger");
                        case "Media" ->
                            getStyleClass().add("tfx-badge-warning");
                        case "Baja" ->
                            getStyleClass().add("tfx-badge-success");
                        default -> {
                        }
                    }
                }
            }
        });

        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnEstado = new Button("Estado");
            private final Button btnAsignar = new Button("Asignar");
            private final Button btnPrioridad = new Button("Prioridad");
            private final HBox box = new HBox(8, btnEditar, btnEstado, btnAsignar, btnPrioridad);

            {
                btnEditar.getStyleClass().add("tfx-icon-btn");
                btnEstado.getStyleClass().add("tfx-icon-btn-secondary");
                btnAsignar.getStyleClass().add("tfx-icon-btn-secondary");
                btnPrioridad.getStyleClass().add("tfx-icon-btn-secondary");

                btnEditar.setOnAction(e -> {
                    Task t = (getTableRow() != null) ? getTableRow().getItem() : null;
                    if (t != null) {
                        openTaskForm(t);
                    }
                });

                btnEstado.setOnAction(e -> {
                    Task t = (getTableRow() != null) ? getTableRow().getItem() : null;
                    if (t != null) {
                        cambiarEstado(t);
                    }
                });

                btnAsignar.setOnAction(e -> {
                    Task t = (getTableRow() != null) ? getTableRow().getItem() : null;
                    if (t != null) {
                        asignarEmpleado(t);
                    }
                });

                btnPrioridad.setOnAction(e -> {
                    Task t = (getTableRow() != null) ? getTableRow().getItem() : null;
                    if (t != null) {
                        cambiarPrioridad(t);
                    }
                });

            }

            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setGraphic(null);
                    return;
                }

                btnEditar.setVisible(isAdmin);
                btnEditar.setManaged(isAdmin);
                btnAsignar.setVisible(isAdmin);
                btnAsignar.setManaged(isAdmin);
                btnPrioridad.setVisible(isAdmin);
                btnPrioridad.setManaged(isAdmin);

                setGraphic(box);
            }
        });

        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(10, 25, 50, 100));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(25));
        }

        smartTable = new SmartTable<>(
                tblTareas,
                masterData,
                null,
                cmbPageSize,
                lblResumen,
                "tareas",
                null
        );

        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblTareas.getFixedCellSize() + 2;

            tblTareas.setPrefHeight(tableHeight);
            tblTareas.setMinHeight(Region.USE_PREF_SIZE);
            tblTareas.setMaxHeight(Region.USE_PREF_SIZE);
        });

        smartTable.addColumnFilter(filterClienteField, (t, text) -> safe(t.getClienteNombre()).contains(text));
        smartTable.addColumnFilter(filterVehiculoField, (t, text) -> safe(t.getVehiculoMatricula()).contains(text));
        smartTable.addColumnFilter(filterProductosField, (t, text) -> safe(t.getRepairDescripcion()).contains(text));
        smartTable.addColumnFilter(filterEmpleadoField, (t, text) -> safe(employeeLabel(t)).contains(text));

        if (filterEstadoCombo != null) {
            filterEstadoCombo.getItems().clear();
            filterEstadoCombo.getItems().add("Todos");
            for (TaskStatus status : TaskStatus.values()) {
                filterEstadoCombo.getItems().add(mapStatusToLabel(status));
            }
            filterEstadoCombo.getSelectionModel().selectFirst();

            smartTable.addColumnFilter(filterEstadoCombo, (t, selected) -> {
                if (selected == null || "Todos".equalsIgnoreCase(selected)) {
                    return true;
                }
                return mapStatusToLabel(t.getEstado()).equalsIgnoreCase(selected);
            });
        }

        if (filterPrioridadCombo != null) {
            filterPrioridadCombo.getItems().clear();
            filterPrioridadCombo.getItems().add("Todas");
            for (TaskPriority priority : TaskPriority.values()) {
                filterPrioridadCombo.getItems().add(mapPriorityToLabel(priority));
            }
            filterPrioridadCombo.getSelectionModel().selectFirst();

            smartTable.addColumnFilter(filterPrioridadCombo, (t, selected) -> {
                if (selected == null || "Todas".equalsIgnoreCase(selected)) {
                    return true;
                }
                return mapPriorityToLabel(t.getPrioridad()).equalsIgnoreCase(selected);
            });
        }

        if (isSuperAdmin && filterEmpresaCombo != null) {
            smartTable.addColumnFilter(filterEmpresaCombo, (t, selected) -> {
                if (selected == null || "Todas".equalsIgnoreCase(selected)) {
                    return true;
                }
                return safeRaw(t.getEmpresaNombre()).equalsIgnoreCase(selected);
            });
        }

        setupRowDoubleClick();
        loadTasksFromDb();
    }

    @FXML
    private void onNuevaTarea() {
        openTaskForm(null);
    }

    @FXML
    private void onRefrescar() {
        loadTasksFromDb();
    }

    @FXML
    private void onLimpiarFiltros() {
        if (filterClienteField != null) {
            filterClienteField.clear();
        }
        if (filterVehiculoField != null) {
            filterVehiculoField.clear();
        }
        if (filterProductosField != null) {
            filterProductosField.clear();
        }
        if (filterEmpleadoField != null) {
            filterEmpleadoField.clear();
        }
        if (filterEstadoCombo != null) {
            filterEstadoCombo.getSelectionModel().selectFirst();
        }
        if (filterPrioridadCombo != null) {
            filterPrioridadCombo.getSelectionModel().selectFirst();
        }
        if (AuthContext.isSuperAdmin() && filterEmpresaCombo != null) {
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }
        if (smartTable != null) {
            smartTable.refresh();
        }
    }

    private void loadTasksFromDb() {
        List<Task> tasks = listTasksUseCase.execute();
        tasks.sort(Comparator.comparing(t -> Optional.ofNullable(t.getCreatedAt()).orElse(java.time.LocalDateTime.MIN), Comparator.reverseOrder()));
        masterData.setAll(tasks);

        if (AuthContext.isSuperAdmin() && filterEmpresaCombo != null) {
            var empresas = tasks.stream().map(Task::getEmpresaNombre).filter(s -> s != null && !s.isBlank()).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            filterEmpresaCombo.setItems(FXCollections.observableArrayList(empresas));
            filterEmpresaCombo.getItems().add(0, "Todas");
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }

        smartTable.refresh();

        if (lblHeaderInfo != null) {
            lblHeaderInfo.setText(masterData.size() + " tareas registradas");
        }
    }

    private void setupRowDoubleClick() {
        tblTareas.setRowFactory(tv -> {
            TableRow<Task> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openTaskForm(row.getItem());
                }
            });
            return row;
        });
    }

    private void openTaskForm(Task task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/TareaFormView.fxml"));
            Parent root = loader.load();
            TareaFormController controller = loader.getController();
            controller.init(AuthContext.getEmpresaId(), saveTaskUseCase, task);

            Stage stage = new Stage();
            stage.setTitle(task == null ? "Nueva tarea" : "Editar tarea");
            stage.initOwner(tblTareas.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadTasksFromDb();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario: " + ex.getMessage()).showAndWait();
        }
    }

    private void cambiarEstado(Task task) {
        if (task == null) {
            return;
        }

        if (AuthContext.isEmpleado() && !isAssignedToCurrentUser(task)) {
            new Alert(Alert.AlertType.WARNING, "Solo puedes cambiar el estado de tus tareas.").showAndWait();
            return;
        }

        List<String> options = FXCollections.observableArrayList();
        for (TaskStatus status : TaskStatus.values()) {
            options.add(mapStatusToLabel(status));
        }

        String currentLabel = mapStatusToLabel(task.getEstado());
        ChoiceDialog<String> dialog = new ChoiceDialog<>(currentLabel, options);
        dialog.setTitle("Cambiar estado");
        dialog.setHeaderText("Selecciona el nuevo estado de la tarea");
        dialog.setContentText("Estado:");

        dialog.showAndWait().ifPresent(selected -> {
            TaskStatus newStatus = mapLabelToStatus(selected);
            if (newStatus == null) {
                return;
            }
            try {
                long empresaId = task.getEmpresaId();
                changeTaskStatusUseCase.execute(task.getId(), empresaId, newStatus);
                loadTasksFromDb();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "No se pudo actualizar el estado: " + ex.getMessage()).showAndWait();
            }
        });
    }

    private void asignarEmpleado(Task task) {
        if (task == null) {
            return;
        }

        Long empresaId = resolveEmpresaForAssignment(task);
        if (empresaId == null) {
            new Alert(Alert.AlertType.WARNING, "Selecciona una empresa para asignar la tarea.").showAndWait();
            return;
        }

        if (task.getEmpresaId() != null && !task.getEmpresaId().equals(empresaId)) {
            new Alert(Alert.AlertType.WARNING, "La tarea pertenece a otra empresa.").showAndWait();
            return;
        }

        List<EmployeeOption> employees = fetchEmployees(empresaId);
        if (employees.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No hay empleados disponibles para asignar.").showAndWait();
            return;
        }

        List<String> options = employees.stream().map(EmployeeOption::label).toList();
        String currentLabel = employees.stream()
                .filter(opt -> opt.id != null && opt.id.equals(task.getAsignadoA()))
                .map(EmployeeOption::label)
                .findFirst()
                .orElse("Sin asignar");

        ChoiceDialog<String> dialog = new ChoiceDialog<>(currentLabel, options);
        dialog.setTitle("Asignar tarea");
        dialog.setHeaderText("Selecciona el empleado para la tarea");
        dialog.setContentText("Empleado:");

        dialog.showAndWait().ifPresent(selected -> {
            EmployeeOption chosen = employees.stream().filter(opt -> opt.label().equals(selected)).findFirst().orElse(null);
            if (chosen == null || chosen.id == null) {
                return;
            }
            try {
                assignTaskUseCase.execute(task.getId(), empresaId, chosen.id);
                loadTasksFromDb();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "No se pudo asignar la tarea: " + ex.getMessage()).showAndWait();
            }
        });
    }

    private void cambiarPrioridad(Task task) {
        if (task == null) {
            return;
        }

        List<String> options = FXCollections.observableArrayList();
        for (TaskPriority priority : TaskPriority.values()) {
            options.add(mapPriorityToLabel(priority));
        }

        String currentLabel = mapPriorityToLabel(task.getPrioridad());
        ChoiceDialog<String> dialog = new ChoiceDialog<>(currentLabel, options);
        dialog.setTitle("Cambiar prioridad");
        dialog.setHeaderText("Selecciona la nueva prioridad");
        dialog.setContentText("Prioridad:");

        dialog.showAndWait().ifPresent(selected -> {
            TaskPriority newPriority = mapLabelToPriority(selected);
            if (newPriority == null) {
                return;
            }
            try {
                long empresaId = task.getEmpresaId();
                setTaskPriorityUseCase.execute(task.getId(), empresaId, newPriority);
                loadTasksFromDb();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "No se pudo actualizar la prioridad: " + ex.getMessage()).showAndWait();
            }
        });
    }

    private boolean isAssignedToCurrentUser(Task task) {
        if (task == null) {
            return false;
        }
        Long asignadoA = task.getAsignadoA();
        if (asignadoA == null) {
            return false;
        }
        return asignadoA.equals(AuthContext.getCurrentUser().getId());
    }

    private String trimDescripcion(String titulo) {
        if (titulo == null) {
            return "";
        }
        return titulo.length() > 40 ? titulo.substring(0, 37) + "..." : titulo;
    }

    private String employeeLabel(Task task) {
        if (task == null) {
            return "";
        }
        String nombre = safeRaw(task.getEmpleadoNombre());
        if (!nombre.isBlank()) {
            return nombre;
        }
        return task.getAsignadoA() == null ? "Sin asignar" : "Empleado #" + task.getAsignadoA();
    }

    private String mapStatusToLabel(TaskStatus status) {
        if (status == null) {
            return "Pendiente";
        }
        return switch (status) {
            case PENDIENTE ->
                "Pendiente";
            case EN_PROCESO ->
                "En proceso";
            case COMPLETADA ->
                "Completada";
            case CANCELADA ->
                "Cancelada";
        };
    }

    private TaskStatus mapLabelToStatus(String label) {
        if (label == null) {
            return null;
        }
        return switch (label) {
            case "Pendiente" ->
                TaskStatus.PENDIENTE;
            case "En proceso" ->
                TaskStatus.EN_PROCESO;
            case "Completada" ->
                TaskStatus.COMPLETADA;
            case "Cancelada" ->
                TaskStatus.CANCELADA;
            default ->
                null;
        };
    }

    private String mapPriorityToLabel(TaskPriority priority) {
        if (priority == null) {
            return "Media";
        }
        return switch (priority) {
            case BAJA ->
                "Baja";
            case MEDIA ->
                "Media";
            case ALTA ->
                "Alta";
        };
    }

    private TaskPriority mapLabelToPriority(String label) {
        if (label == null) {
            return null;
        }
        return switch (label) {
            case "Baja" ->
                TaskPriority.BAJA;
            case "Media" ->
                TaskPriority.MEDIA;
            case "Alta" ->
                TaskPriority.ALTA;
            default ->
                null;
        };
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private String safeRaw(String value) {
        return value == null ? "" : value.trim();
    }

    private List<EmployeeOption> fetchEmployees(Long empresaId) {
        if (empresaId == null) {
            return List.of();
        }
        MySqlUserRepository repo = new MySqlUserRepository();
        List<User> users = repo.findByEmpresaId(empresaId);
        List<EmployeeOption> result = users.stream().filter(u -> u.getRol() == UserRole.EMPLEADO || u.getRol() == UserRole.ADMIN).map(u -> new EmployeeOption(u.getId(), u.getNombre() + " (ID " + u.getId() + ")")).toList();
        List<EmployeeOption> output = FXCollections.observableArrayList();
        output.add(new EmployeeOption(null, "Sin asignar"));
        output.addAll(result);
        return output;
    }

    private Long resolveEmpresaForAssignment(Task task) {
        Long empresaId = task != null ? task.getEmpresaId() : null;
        if (!AuthContext.isSuperAdmin()) {
            return empresaId != null ? empresaId : AuthContext.getEmpresaId();
        }

        Long empresaFromFilter = resolveEmpresaFromFilter();
        if (empresaFromFilter != null) {
            return empresaFromFilter;
        }

        List<EmpresaOption> empresas = fetchEmpresas();
        if (empresas.isEmpty()) {
            return empresaId;
        }

        EmpresaOption selected = empresas.stream().filter(e -> empresaId != null && e.id == empresaId).findFirst().orElse(empresas.get(0));
        List<String> options = empresas.stream().map(EmpresaOption::label).toList();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(selected.label(), options);
        dialog.setTitle("Seleccionar empresa");
        dialog.setHeaderText("Selecciona la empresa para filtrar empleados");
        dialog.setContentText("Empresa:");

        return dialog.showAndWait().flatMap(choice -> empresas.stream().filter(e -> e.label().equals(choice)).findFirst()).map(e -> e.id).orElse(empresaId);
    }

    private Long resolveEmpresaFromFilter() {
        if (filterEmpresaCombo == null) {
            return null;
        }
        String selected = filterEmpresaCombo.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isBlank() || "Todas".equalsIgnoreCase(selected)) {
            return null;
        }
        return fetchEmpresas().stream().filter(e -> e.label().equalsIgnoreCase(selected)).map(e -> e.id).findFirst().orElse(null);
    }

    private List<EmpresaOption> fetchEmpresas() {
        var repo = new com.gearmind.infrastructure.company.MySqlEmpresaRepository();
        return repo.findAll().stream().map(c -> new EmpresaOption(c.getId(), c.getNombre())).toList();
    }

    private record EmpresaOption(long id, String label) {
    }

    private record EmployeeOption(Long id, String label) {
    }
}
