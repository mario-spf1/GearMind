package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.task.SaveTaskRequest;
import com.gearmind.application.task.SaveTaskUseCase;
import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.task.Task;
import com.gearmind.domain.task.TaskPriority;
import com.gearmind.domain.task.TaskStatus;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import com.gearmind.infrastructure.repair.MySqlRepairRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class TareaFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private HBox boxEmpresa;
    @FXML
    private ComboBox<EmpresaOption> cmbEmpresa;
    @FXML
    private ComboBox<RepairOption> cbReparacion;
    @FXML
    private ComboBox<EmployeeOption> cbEmpleado;
    @FXML
    private ComboBox<String> cbPrioridad;
    @FXML
    private TextField txtTitulo;
    @FXML
    private TextField txtFechaLimite;
    @FXML
    private TextArea txtDescripcion;

    private Long empresaId;
    private SaveTaskUseCase saveTaskUseCase;
    private Task existingTask;
    private boolean saved = false;

    private final ObservableList<EmpresaOption> empresas = FXCollections.observableArrayList();
    private FilteredList<EmpresaOption> empresasFiltradas;
    private final ObservableList<RepairOption> allRepairs = FXCollections.observableArrayList();
    private FilteredList<RepairOption> filteredRepairs;
    private final ObservableList<EmployeeOption> allEmployees = FXCollections.observableArrayList();
    private FilteredList<EmployeeOption> filteredEmployees;

    private boolean settingEmpresaProgrammatically = false;
    private boolean settingReparacionProgrammatically = false;
    private boolean settingEmpleadoProgrammatically = false;

    public void init(Long empresaId, SaveTaskUseCase saveTaskUseCase, Task existingTask) {
        this.empresaId = empresaId;
        this.saveTaskUseCase = saveTaskUseCase;
        this.existingTask = existingTask;

        configureEmpresaCombo();
        configurePrioridadCombo();
        configureRepairCombo();
        configureEmpleadoCombo();

        if (existingTask != null) {
            lblTitulo.setText("Editar tarea");
            txtTitulo.setText(existingTask.getTitulo());
            txtDescripcion.setText(existingTask.getDescripcion());
            if (existingTask.getPrioridad() != null) {
                cbPrioridad.getSelectionModel().select(mapPriorityToLabel(existingTask.getPrioridad()));
            }
            if (existingTask.getFechaLimite() != null) {
                txtFechaLimite.setText(formatFechaLimite(existingTask.getFechaLimite()));
            }

            if (AuthContext.isSuperAdmin()) {
                selectEmpresa(existingTask.getEmpresaId());
            }

            selectRepair(existingTask.getOrdenTrabajoId());
            selectEmployee(existingTask.getAsignadoA());
        } else {
            lblTitulo.setText("Nueva tarea");
            cbPrioridad.getSelectionModel().select(mapPriorityToLabel(TaskPriority.MEDIA));
            if (AuthContext.isEmpleado()) {
                selectSelfEmployee();
            }
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onGuardar() {
        try {
            SaveTaskRequest request = buildRequest();
            saveTaskUseCase.execute(request);
            saved = true;
            close();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onCancelar() {
        close();
    }

    private SaveTaskRequest buildRequest() {
        SaveTaskRequest request = new SaveTaskRequest();
        if (existingTask != null) {
            request.setId(existingTask.getId());
        }

        Long empresaSeleccionada = resolveEmpresaSeleccionada();
        request.setEmpresaId(empresaSeleccionada);

        RepairOption repairOption = cbReparacion.getValue();
        if (repairOption != null) {
            request.setOrdenTrabajoId(repairOption.id);
        }

        EmployeeOption empleadoOption = cbEmpleado.getValue();
        if (empleadoOption != null) {
            request.setAsignadoA(empleadoOption.id);
        }

        request.setTitulo(txtTitulo.getText());
        request.setDescripcion(txtDescripcion.getText());
        request.setEstado(existingTask != null ? existingTask.getEstado() : TaskStatus.PENDIENTE);
        request.setPrioridad(mapLabelToPriority(cbPrioridad.getValue()));
        request.setFechaLimite(parseFechaLimite(txtFechaLimite.getText()));
        return request;
    }

    private Long resolveEmpresaSeleccionada() {
        if (!AuthContext.isSuperAdmin()) {
            return empresaId;
        }
        EmpresaOption opt = cmbEmpresa.getValue();
        return opt != null ? opt.id : null;
    }

    private void configureEmpresaCombo() {
        if (!AuthContext.isSuperAdmin()) {
            if (boxEmpresa != null) {
                boxEmpresa.setVisible(false);
                boxEmpresa.setManaged(false);
            }
            return;
        }

        empresas.clear();
        empresas.addAll(fetchEmpresas());

        empresasFiltradas = new FilteredList<>(empresas, opt -> true);
        cmbEmpresa.setItems(empresasFiltradas);
        cmbEmpresa.setEditable(true);
        cmbEmpresa.setConverter(new StringConverter<>() {
            @Override
            public String toString(EmpresaOption object) {
                return object == null ? "" : object.nombre;
            }

            @Override
            public EmpresaOption fromString(String string) {
                if (string == null) {
                    return null;
                }
                String s = string.trim().toLowerCase(Locale.ROOT);
                if (s.isBlank()) {
                    return null;
                }
                return empresas.stream().filter(o -> o.nombre.toLowerCase(Locale.ROOT).equals(s)).findFirst().orElse(null);
            }
        });

        cmbEmpresa.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            if (settingEmpresaProgrammatically) {
                return;
            }

            EmpresaOption selected = cmbEmpresa.getSelectionModel().getSelectedItem();
            String nt = (newV == null ? "" : newV).trim();

            if (selected != null && selected.nombre != null && selected.nombre.equalsIgnoreCase(nt)) {
                return;
            }

            String filtro = nt.toLowerCase(Locale.ROOT);
            settingEmpresaProgrammatically = true;
            try {
                empresasFiltradas.setPredicate(opt -> filtro.isBlank() || opt.nombre.toLowerCase(Locale.ROOT).contains(filtro));
            } finally {
                settingEmpresaProgrammatically = false;
            }

            if (cmbEmpresa.isFocused() && !cmbEmpresa.isShowing()) {
                cmbEmpresa.show();
            }
        });

        cmbEmpresa.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            settingEmpresaProgrammatically = true;
            try {
                cmbEmpresa.getEditor().setText(newVal.nombre);
            } finally {
                settingEmpresaProgrammatically = false;
            }
            loadRepairsAndEmployees(newVal.id);
        });

        cmbEmpresa.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(EmpresaOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.nombre);
            }
        });
        cmbEmpresa.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(EmpresaOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.nombre);
            }
        });

        if (!empresas.isEmpty()) {
            cmbEmpresa.getSelectionModel().selectFirst();
            loadRepairsAndEmployees(cmbEmpresa.getValue().id);
        }
    }

    private void configureRepairCombo() {
        filteredRepairs = new FilteredList<>(allRepairs, opt -> true);
        cbReparacion.setItems(filteredRepairs);
        cbReparacion.setEditable(true);
        cbReparacion.setConverter(new StringConverter<>() {
            @Override
            public String toString(RepairOption object) {
                return object == null ? "" : object.label;
            }

            @Override
            public RepairOption fromString(String string) {
                if (string == null) {
                    return null;
                }
                String s = string.trim().toLowerCase(Locale.ROOT);
                if (s.isBlank()) {
                    return null;
                }
                return allRepairs.stream().filter(o -> o.label.toLowerCase(Locale.ROOT).equals(s)).findFirst().orElse(null);
            }
        });

        cbReparacion.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            if (settingReparacionProgrammatically) {
                return;
            }

            RepairOption selected = cbReparacion.getSelectionModel().getSelectedItem();
            String nt = (newV == null ? "" : newV).trim();

            if (selected != null && selected.label != null && selected.label.equalsIgnoreCase(nt)) {
                return;
            }

            String filtro = nt.toLowerCase(Locale.ROOT);
            settingReparacionProgrammatically = true;
            try {
                filteredRepairs.setPredicate(opt -> filtro.isBlank() || opt.label.toLowerCase(Locale.ROOT).contains(filtro));
            } finally {
                settingReparacionProgrammatically = false;
            }

            if (cbReparacion.isFocused() && !cbReparacion.isShowing()) {
                cbReparacion.show();
            }
        });

        cbReparacion.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            settingReparacionProgrammatically = true;
            try {
                cbReparacion.getEditor().setText(newVal.label);
            } finally {
                settingReparacionProgrammatically = false;
            }
        });

        cbReparacion.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(RepairOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.label);
            }
        });
        cbReparacion.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(RepairOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.label);
            }
        });

        if (!AuthContext.isSuperAdmin()) {
            loadRepairsAndEmployees(empresaId);
        }
    }

    private void configureEmpleadoCombo() {
        filteredEmployees = new FilteredList<>(allEmployees, opt -> true);
        cbEmpleado.setItems(filteredEmployees);
        cbEmpleado.setEditable(true);
        cbEmpleado.setConverter(new StringConverter<>() {
            @Override
            public String toString(EmployeeOption object) {
                return object == null ? "" : object.label;
            }

            @Override
            public EmployeeOption fromString(String string) {
                if (string == null) {
                    return null;
                }
                String s = string.trim().toLowerCase(Locale.ROOT);
                if (s.isBlank()) {
                    return null;
                }
                return allEmployees.stream().filter(o -> o.label.toLowerCase(Locale.ROOT).equals(s)).findFirst().orElse(null);
            }
        });

        cbEmpleado.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            if (settingEmpleadoProgrammatically) {
                return;
            }

            EmployeeOption selected = cbEmpleado.getSelectionModel().getSelectedItem();
            String nt = (newV == null ? "" : newV).trim();

            if (selected != null && selected.label != null && selected.label.equalsIgnoreCase(nt)) {
                return;
            }

            String filtro = nt.toLowerCase(Locale.ROOT);
            settingEmpleadoProgrammatically = true;
            try {
                filteredEmployees.setPredicate(opt -> filtro.isBlank() || opt.label.toLowerCase(Locale.ROOT).contains(filtro));
            } finally {
                settingEmpleadoProgrammatically = false;
            }

            if (cbEmpleado.isFocused() && !cbEmpleado.isShowing()) {
                cbEmpleado.show();
            }
        });

        cbEmpleado.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            settingEmpleadoProgrammatically = true;
            try {
                cbEmpleado.getEditor().setText(newVal.label);
            } finally {
                settingEmpleadoProgrammatically = false;
            }
        });

        cbEmpleado.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(EmployeeOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.label);
            }
        });
        cbEmpleado.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(EmployeeOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.label);
            }
        });
    }

    private void configurePrioridadCombo() {
        cbPrioridad.getItems().clear();
        for (TaskPriority priority : TaskPriority.values()) {
            cbPrioridad.getItems().add(mapPriorityToLabel(priority));
        }
    }

    private void loadRepairsAndEmployees(Long empresaId) {
        allRepairs.clear();
        allEmployees.clear();

        if (empresaId == null) {
            cbReparacion.getSelectionModel().clearSelection();
            cbEmpleado.getSelectionModel().clearSelection();
            return;
        }

        MySqlRepairRepository repairRepository = new MySqlRepairRepository();
        List<Repair> repairs = repairRepository.findByEmpresa(empresaId);
        for (Repair repair : repairs) {
            String label = buildRepairLabel(repair);
            allRepairs.add(new RepairOption(repair.getId(), label));
        }

        MySqlUserRepository userRepository = new MySqlUserRepository();
        List<User> users = userRepository.findByEmpresaId(empresaId);
        allEmployees.add(new EmployeeOption(null, "Sin asignar"));
        for (User user : users) {
            if (user.getRol() != UserRole.EMPLEADO) {
                continue;
            }
            allEmployees.add(new EmployeeOption(user.getId(), user.getNombre() + " (ID " + user.getId() + ")"));
        }

        if (AuthContext.isEmpleado()) {
            selectSelfEmployee();
        }
    }

    private void selectEmpresa(Long empresaId) {
        if (!AuthContext.isSuperAdmin() || empresaId == null) {
            return;
        }

        EmpresaOption opt = empresas.stream().filter(e -> e.id == empresaId).findFirst().orElse(null);
        if (opt != null) {
            settingEmpresaProgrammatically = true;
            try {
                cmbEmpresa.getSelectionModel().select(opt);
                if (cmbEmpresa.isEditable()) {
                    cmbEmpresa.getEditor().setText(opt.nombre);
                }
            } finally {
                settingEmpresaProgrammatically = false;
            }
            loadRepairsAndEmployees(opt.id);
        }
    }

    private void selectRepair(Long repairId) {
        if (repairId == null) {
            return;
        }
        RepairOption opt = allRepairs.stream().filter(r -> r.id.equals(repairId)).findFirst().orElse(null);
        if (opt != null) {
            settingReparacionProgrammatically = true;
            try {
                cbReparacion.setValue(opt);
                if (cbReparacion.isEditable()) {
                    cbReparacion.getEditor().setText(opt.label);
                }
            } finally {
                settingReparacionProgrammatically = false;
            }
        }
    }

    private void selectEmployee(Long employeeId) {
        if (employeeId == null) {
            return;
        }
        EmployeeOption opt = allEmployees.stream().filter(e -> employeeId.equals(e.id)).findFirst().orElse(null);
        if (opt != null) {
            settingEmpleadoProgrammatically = true;
            try {
                cbEmpleado.setValue(opt);
                if (cbEmpleado.isEditable()) {
                    cbEmpleado.getEditor().setText(opt.label);
                }
            } finally {
                settingEmpleadoProgrammatically = false;
            }
        }
    }

    private void selectSelfEmployee() {
        User current = AuthContext.getCurrentUser();
        if (current == null) {
            return;
        }
        EmployeeOption self = allEmployees.stream().filter(e -> current.getId() == (e.id != null ? e.id : -1)).findFirst().orElse(null);
        if (self != null) {
            settingEmpleadoProgrammatically = true;
            try {
                cbEmpleado.setValue(self);
                cbEmpleado.setDisable(true);
                cbEmpleado.setEditable(false);
            } finally {
                settingEmpleadoProgrammatically = false;
            }
        }
    }

    private String buildRepairLabel(Repair repair) {
        StringBuilder sb = new StringBuilder();
        if (repair.getId() != null) {
            sb.append("#").append(repair.getId());
        }
        if (repair.getClienteNombre() != null && !repair.getClienteNombre().isBlank()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(repair.getClienteNombre());
        }
        if (repair.getVehiculoEtiqueta() != null && !repair.getVehiculoEtiqueta().isBlank()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(repair.getVehiculoEtiqueta());
        }
        if (repair.getDescripcion() != null && !repair.getDescripcion().isBlank()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(repair.getDescripcion());
        }
        return sb.toString();
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
            return TaskPriority.MEDIA;
        }
        return switch (label) {
            case "Baja" ->
                TaskPriority.BAJA;
            case "Media" ->
                TaskPriority.MEDIA;
            case "Alta" ->
                TaskPriority.ALTA;
            default ->
                TaskPriority.MEDIA;
        };
    }

    private List<EmpresaOption> fetchEmpresas() {
        var repo = new com.gearmind.infrastructure.company.MySqlEmpresaRepository();
        return repo.findAll().stream().map(c -> new EmpresaOption(c.getId(), c.getNombre())).toList();
    }

    private LocalDateTime parseFechaLimite(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha límite debe tener el formato dd/MM/yyyy HH:mm.");
        }
    }

    private String formatFechaLimite(LocalDateTime fechaLimite) {
        if (fechaLimite == null) {
            return "";
        }
        return fechaLimite.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private void close() {
        Stage stage = (Stage) lblTitulo.getScene().getWindow();
        stage.close();
    }

    private static class EmpresaOption {

        private final long id;
        private final String nombre;

        private EmpresaOption(long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private static class RepairOption {

        private final Long id;
        private final String label;

        private RepairOption(Long id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class EmployeeOption {

        private final Long id;
        private final String label;

        private EmployeeOption(Long id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
