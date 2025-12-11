package com.gearmind.presentation.controller;

import com.gearmind.application.appointment.ChangeAppointmentStatusUseCase;
import com.gearmind.application.appointment.ListAppointmentsUseCase;
import com.gearmind.application.appointment.SaveAppointmentUseCase;
import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentOrigin;
import com.gearmind.domain.appointment.AppointmentStatus;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.appointment.MySqlAppointmentRepository;
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
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CitasController {

    @FXML
    private TabPane tabPane;
    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Button btnNuevaCita;
    @FXML
    private Button btnRefrescar;

    @FXML
    private DatePicker dpAgendaFecha;
    @FXML
    private ComboBox<String> cbAgendaEstado;
    @FXML
    private ListView<AgendaSlot> lstAgenda;

    @FXML
    private TableView<Appointment> tblCitas;
    @FXML
    private TableColumn<Appointment, String> colFechaHora;
    @FXML
    private TableColumn<Appointment, String> colCliente;
    @FXML
    private TableColumn<Appointment, String> colVehiculo;
    @FXML
    private TableColumn<Appointment, String> colEmpleado;
    @FXML
    private TableColumn<Appointment, String> colEmpresa;
    @FXML
    private TableColumn<Appointment, String> colEstado;
    @FXML
    private TableColumn<Appointment, String> colOrigen;
    @FXML
    private TableColumn<Appointment, String> colNotas;
    @FXML
    private TableColumn<Appointment, Appointment> colAcciones;

    @FXML
    private TextField filterClienteField;
    @FXML
    private TextField filterVehiculoField;
    @FXML
    private TextField filterEmpleadoField;
    @FXML
    private TextField filterEmpresaField;
    @FXML
    private TextField filterNotasField;
    @FXML
    private ComboBox<String> cbFiltroEstado;
    @FXML
    private ComboBox<String> cbFiltroOrigen;
    @FXML
    private TextField filterEstadoField;
    @FXML
    private TextField filterOrigenField;
    @FXML
    private TextField txtBuscar;
    @FXML
    private Label lblResumen;

    private final ObservableList<Appointment> masterData = FXCollections.observableArrayList();
    private final ListAppointmentsUseCase listAppointmentsUseCase;
    private final SaveAppointmentUseCase saveAppointmentUseCase;
    private final ChangeAppointmentStatusUseCase changeAppointmentStatusUseCase;
    private SmartTable<Appointment> smartTable;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    public CitasController() {
        MySqlAppointmentRepository repo = new MySqlAppointmentRepository();
        this.listAppointmentsUseCase = new ListAppointmentsUseCase(repo);
        this.saveAppointmentUseCase = new SaveAppointmentUseCase(repo);
        this.changeAppointmentStatusUseCase = new ChangeAppointmentStatusUseCase(repo);
    }

    @FXML
    private void initialize() {
        configureAgendaControls();
        configureAgendaList();
        configureTable();
        tblCitas.setFixedCellSize(28);
        smartTable = new SmartTable<>(tblCitas, masterData, txtBuscar, cmbPageSize, lblResumen, "citas", this::matchesGlobalFilter);

        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblCitas.getFixedCellSize() + 2;
            tblCitas.setPrefHeight(tableHeight);
        });

        smartTable.addColumnFilter(filterClienteField, (cita, text) -> appointmentValueOrBlank(cita.getCustomerId()).contains(text));
        smartTable.addColumnFilter(filterVehiculoField, (cita, text) -> appointmentValueOrBlank(cita.getVehicleId()).contains(text));
        smartTable.addColumnFilter(filterEmpleadoField, (cita, text) -> appointmentValueOrBlank(cita.getEmployeeId()).contains(text));
        smartTable.addColumnFilter(filterEmpresaField, (cita, text) -> appointmentValueOrBlank(cita.getEmpresaId()).contains(text));
        smartTable.addColumnFilter(filterNotasField, (cita, text) -> safe(cita.getNotes()).contains(text));
        smartTable.addColumnFilter(filterEstadoField, (cita, text) -> safe(mapStatusToLabel(cita.getStatus())).contains(text));
        smartTable.addColumnFilter(filterOrigenField, (cita, text) -> safe(mapOriginToLabel(cita.getOrigin())).contains(text));

        configureEstadoYOrigenCombos();
        dpAgendaFecha.setValue(LocalDate.now());
        applyRoleVisibility();
        reloadFromDb();
    }

    private void configureAgendaControls() {
        dpAgendaFecha.valueProperty().addListener((obs, oldV, newV) -> loadAgenda());
        cbAgendaEstado.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> loadAgenda());
    }

    private void configureAgendaList() {
        lstAgenda.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AgendaSlot item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label lblHora = new Label(item.time().format(timeFormatter));
                lblHora.getStyleClass().add("tfx-agenda-hour");
                HBox citasBox = new HBox(6);

                if (item.appointments().isEmpty()) {
                    Label libre = new Label("(Libre)");
                    libre.getStyleClass().add("tfx-muted");
                    citasBox.getChildren().add(libre);
                } else {
                    for (Appointment a : item.appointments()) {
                        String horaReal = a.getDateTime().toLocalTime().format(timeFormatter);
                        String texto = horaReal + " · Cliente " + appointmentValueOrBlank(a.getCustomerId());
                        Button chip = new Button(texto);
                        chip.getStyleClass().add("tfx-agenda-chip");
                        chip.setOnAction(e -> openCitaForm(a));
                        citasBox.getChildren().add(chip);
                    }
                }
                HBox row = new HBox(12, lblHora, citasBox);
                row.setFillHeight(true);
                setText(null);
                setGraphic(row);
            }
        });
    }

    private void configureTable() {
        tblCitas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblCitas.setItems(masterData);

        colFechaHora.setCellValueFactory(cellData -> {
            Appointment appointment = cellData.getValue();
            if (appointment.getDateTime() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(appointment.getDateTime().format(dateTimeFormatter));
        });

        colCliente.setCellValueFactory(cellData -> new SimpleStringProperty(appointmentValueOrBlank(cellData.getValue().getCustomerId())));

        colVehiculo.setCellValueFactory(cellData -> new SimpleStringProperty(appointmentValueOrBlank(cellData.getValue().getVehicleId())));

        colEmpleado.setCellValueFactory(cellData -> new SimpleStringProperty(appointmentValueOrBlank(cellData.getValue().getEmployeeId())));

        colEmpresa.setCellValueFactory(cellData -> new SimpleStringProperty(appointmentValueOrBlank(cellData.getValue().getEmpresaId())));

        colEstado.setCellValueFactory(cellData -> {
            AppointmentStatus status = cellData.getValue().getStatus();
            String text = status != null ? mapStatusToLabel(status) : "";
            return new SimpleStringProperty(text);
        });

        colOrigen.setCellValueFactory(cellData -> {
            AppointmentOrigin origin = cellData.getValue().getOrigin();
            String text = origin != null ? mapOriginToLabel(origin) : "";
            return new SimpleStringProperty(text);
        });

        colNotas.setCellValueFactory(cellData -> {
            String notas = cellData.getValue().getNotes();
            if (notas == null) {
                return new SimpleStringProperty("");
            }
            String trimmed = notas.length() > 40 ? notas.substring(0, 37) + "..." : notas;
            return new SimpleStringProperty(trimmed);
        });

        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnCompletar = new Button("Completar");
            private final HBox box = new HBox(8, btnEditar, btnCompletar);

            {
                btnEditar.getStyleClass().add("tfx-icon-btn");
                btnCompletar.getStyleClass().add("tfx-icon-btn-secondary");

                btnEditar.setOnAction(e -> {
                    Appointment appointment = getItem();
                    if (appointment != null) {
                        openCitaForm(appointment);
                    }
                });

                btnCompletar.setOnAction(e -> {
                    Appointment appointment = getItem();
                    if (appointment != null) {
                        completarCita(appointment);
                    }
                });
            }

            @Override
            protected void updateItem(Appointment appointment, boolean empty) {
                super.updateItem(appointment, empty);
                if (empty || appointment == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    private void configureEstadoYOrigenCombos() {
        cbFiltroEstado.getItems().clear();
        cbFiltroEstado.getItems().add("Todos");
        for (AppointmentStatus status : AppointmentStatus.values()) {
            cbFiltroEstado.getItems().add(mapStatusToLabel(status));
        }
        cbFiltroEstado.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null || "Todos".equals(newV)) {
                filterEstadoField.setText("");
            } else {
                filterEstadoField.setText(newV.toLowerCase(Locale.ROOT));
            }
            smartTable.refresh();
        });
        cbFiltroEstado.getSelectionModel().selectFirst();
        cbFiltroOrigen.getItems().clear();
        cbFiltroOrigen.getItems().add("Todos");
        cbFiltroOrigen.getItems().add(mapOriginToLabel(AppointmentOrigin.INTERNAL));
        cbFiltroOrigen.getItems().add(mapOriginToLabel(AppointmentOrigin.TELEGRAM));
        cbFiltroOrigen.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null || "Todos".equals(newV)) {
                filterOrigenField.setText("");
            } else {
                filterOrigenField.setText(newV.toLowerCase(Locale.ROOT));
            }
            smartTable.refresh();
        });

        cbFiltroOrigen.getSelectionModel().selectFirst();
        cbAgendaEstado.getItems().clear();
        cbAgendaEstado.getItems().add("Todos");

        for (AppointmentStatus status : AppointmentStatus.values()) {
            cbAgendaEstado.getItems().add(mapStatusToLabel(status));
        }
        cbAgendaEstado.getSelectionModel().selectFirst();
    }

    private void applyRoleVisibility() {
        if (!AuthContext.isLoggedIn()) {
            colEmpresa.setVisible(false);
            filterEmpresaField.setVisible(false);
            filterEmpresaField.setManaged(false);
            return;
        }

        UserRole role = AuthContext.getRole();

        if (role != UserRole.SUPER_ADMIN) {

            if (colEmpresa != null) {
                colEmpresa.setVisible(false);
            }

            if (filterEmpresaField != null) {
                filterEmpresaField.setVisible(false);
                filterEmpresaField.setManaged(false);
            }
        }
    }

    @FXML
    private void onNuevaCita() {
        openCitaForm(null);
    }

    @FXML
    private void onRefrescar() {
        reloadFromDb();
    }

    @FXML
    private void onAgendaHoy() {
        dpAgendaFecha.setValue(LocalDate.now());
        loadAgenda();
    }

    private void reloadFromDb() {
        long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
        List<Appointment> citas = listAppointmentsUseCase.execute(empresaId);

        if (AuthContext.isLoggedIn()) {
            UserRole role = AuthContext.getRole();
            User user = AuthContext.getCurrentUser();

            if (role == UserRole.EMPLEADO && user != null) {
                Long userId = user.getId();
                List<Appointment> propias = new ArrayList<>();
                for (Appointment a : citas) {
                    if (a.getEmployeeId() != null && a.getEmployeeId().equals(userId)) {
                        propias.add(a);
                    }
                }
                masterData.setAll(propias);
            } else {
                masterData.setAll(citas);
            }
        } else {
            masterData.setAll(citas);
        }

        smartTable.refresh();
        loadAgenda();
    }

    private void loadAgenda() {
        LocalDate selectedDate = dpAgendaFecha.getValue();
        if (selectedDate == null) {
            lstAgenda.getItems().clear();
            return;
        }

        String estadoSeleccionado = cbAgendaEstado.getSelectionModel().getSelectedItem();
        List<Appointment> citasDia = new ArrayList<>();

        for (Appointment c : masterData) {
            if (c.getDateTime() == null) {
                continue;
            }
            if (!c.getDateTime().toLocalDate().isEqual(selectedDate)) {
                continue;
            }

            if (estadoSeleccionado == null || "Todos".equals(estadoSeleccionado)) {
                citasDia.add(c);
            } else if (mapStatusToLabel(c.getStatus()).equals(estadoSeleccionado)) {
                citasDia.add(c);
            }
        }

        List<AgendaSlot> items = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            java.time.LocalTime slotTime = java.time.LocalTime.of(hour, 0);
            List<Appointment> citasHora = new ArrayList<>();

            for (Appointment c : citasDia) {
                java.time.LocalTime t = c.getDateTime().toLocalTime();
                if (t.getHour() == hour) {
                    citasHora.add(c);
                }
            }
            items.add(new AgendaSlot(slotTime, citasHora));
        }

        lstAgenda.getItems().setAll(items);
        double cellSize = lstAgenda.getFixedCellSize() > 0 ? lstAgenda.getFixedCellSize() : 40;
        lstAgenda.setPrefHeight(items.size() * cellSize + 2);
    }

    private void openCitaForm(Appointment appointment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CitaFormView.fxml"));
            Parent root = loader.load();
            CitaFormController controller = loader.getController();
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            controller.init(empresaId, saveAppointmentUseCase, appointment);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(appointment == null ? "Nueva cita" : "Editar cita");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.isSaved()) {
                reloadFromDb();
            }
        } catch (IOException e) {
            showError("No se ha podido abrir el formulario de cita: " + e.getMessage());
        }
    }

    private void completarCita(Appointment appointment) {
        try {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            changeAppointmentStatusUseCase.execute(appointment.getId(), empresaId, AppointmentStatus.COMPLETED);
            reloadFromDb();
        } catch (Exception e) {
            showError("Error al marcar cita como completada: " + e.getMessage());
        }
    }

    private String appointmentValueOrBlank(Long value) {
        return value != null ? String.valueOf(value) : "";
    }

    private String mapStatusToLabel(AppointmentStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case REQUESTED ->
                "Solicitada";
            case CONFIRMED ->
                "Confirmada";
            case CANCELLED ->
                "Cancelada";
            case COMPLETED ->
                "Completada";
        };
    }

    private String mapOriginToLabel(AppointmentOrigin origin) {
        if (origin == null) {
            return "";
        }
        return switch (origin) {
            case INTERNAL ->
                "Interno";
            case TELEGRAM ->
                "Telegram";
        };
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private boolean matchesGlobalFilter(Appointment c, String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String t = safe(text);

        String haystack = (appointmentValueOrBlank(c.getCustomerId()) + " " + appointmentValueOrBlank(c.getVehicleId()) + " " + appointmentValueOrBlank(c.getEmployeeId()) + " " + appointmentValueOrBlank(c.getEmpresaId()) + " " + safe(c.getNotes()) + " " + safe(mapStatusToLabel(c.getStatus())) + " " + safe(mapOriginToLabel(c.getOrigin())));

        return haystack.contains(t);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record AgendaSlot(java.time.LocalTime time, List<Appointment> appointments) {

    }
}
