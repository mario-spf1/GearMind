package com.gearmind.presentation.controller;

import com.gearmind.application.appointment.ChangeAppointmentStatusUseCase;
import com.gearmind.application.appointment.ListAppointmentsUseCase;
import com.gearmind.application.appointment.SaveAppointmentRequest;
import com.gearmind.application.appointment.SaveAppointmentUseCase;
import com.gearmind.application.common.SessionManager;
import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentOrigin;
import com.gearmind.domain.appointment.AppointmentStatus;
import com.gearmind.infrastructure.appointment.MySqlAppointmentRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CitasController {

    @FXML
    private TabPane tabPane;

    @FXML
    private Button btnNuevaCita;

    @FXML
    private Button btnRefrescar;

    @FXML
    private DatePicker dpAgendaFecha;

    @FXML
    private ComboBox<String> cbAgendaEstado;

    @FXML
    private ListView<Appointment> lstAgenda;

    @FXML
    private TableView<Appointment> tblCitas;

    @FXML
    private TableColumn<Appointment, String> colFechaHora;

    @FXML
    private TableColumn<Appointment, String> colCliente;

    @FXML
    private TableColumn<Appointment, String> colVehiculo;

    @FXML
    private TableColumn<Appointment, String> colEstado;

    @FXML
    private TableColumn<Appointment, String> colOrigen;

    @FXML
    private TableColumn<Appointment, String> colNotas;

    @FXML
    private TableColumn<Appointment, Appointment> colAcciones;

    @FXML
    private TextField txtFiltroTexto;

    @FXML
    private ComboBox<String> cbFiltroEstado;

    @FXML
    private DatePicker dpFiltroDesde;

    @FXML
    private DatePicker dpFiltroHasta;

    private final ObservableList<Appointment> citas = FXCollections.observableArrayList();
    private ListAppointmentsUseCase listAppointmentsUseCase;
    private SaveAppointmentUseCase saveAppointmentUseCase;
    private ChangeAppointmentStatusUseCase changeAppointmentStatusUseCase;
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault());

    @FXML
    private void initialize() {
        var appointmentRepository = new MySqlAppointmentRepository();
        this.listAppointmentsUseCase = new ListAppointmentsUseCase(appointmentRepository);
        this.saveAppointmentUseCase = new SaveAppointmentUseCase(appointmentRepository);
        this.changeAppointmentStatusUseCase = new ChangeAppointmentStatusUseCase(appointmentRepository);
        configureTable();
        configureEstadoCombos();
        dpAgendaFecha.setValue(LocalDate.now());
        loadCitas();
    }

    private void configureTable() {
        tblCitas.setItems(citas);
        tblCitas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colFechaHora.setCellValueFactory(cellData -> {
            var appointment = cellData.getValue();
            if (appointment.getDateTime() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(appointment.getDateTime().format(dateTimeFormatter));
        });

        colCliente.setCellValueFactory(cellData -> new SimpleStringProperty(appointmentValueOrBlank(cellData.getValue().getCustomerId())));
        colVehiculo.setCellValueFactory(cellData -> new SimpleStringProperty(appointmentValueOrBlank(cellData.getValue().getVehicleId())));
        
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

            {
                btnEditar.getStyleClass().add("button-table");
                btnCompletar.getStyleClass().add("button-table-secondary");

                btnEditar.setOnAction(e -> {
                    Appointment appointment = getTableRow().getItem();
                    if (appointment != null) {
                        onEditarCita(appointment);
                    }
                });

                btnCompletar.setOnAction(e -> {
                    Appointment appointment = getTableRow().getItem();
                    if (appointment != null) {
                        onCompletarCita(appointment);
                    }
                });
            }

            @Override
            protected void updateItem(Appointment appointment, boolean empty) {
                super.updateItem(appointment, empty);
                if (empty || appointment == null) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8, btnEditar, btnCompletar);
                    setGraphic(box);
                }
            }
        });
    }

    private void configureEstadoCombos() {
        cbFiltroEstado.getItems().clear();
        cbFiltroEstado.getItems().add("Todos");
        for (AppointmentStatus status : AppointmentStatus.values()) {
            cbFiltroEstado.getItems().add(mapStatusToLabel(status));
        }
        cbFiltroEstado.getSelectionModel().selectFirst();

        cbAgendaEstado.getItems().clear();
        cbAgendaEstado.getItems().add("Todos");
        for (AppointmentStatus status : AppointmentStatus.values()) {
            cbAgendaEstado.getItems().add(mapStatusToLabel(status));
        }
        cbAgendaEstado.getSelectionModel().selectFirst();
    }

    @FXML
    private void onNuevaCita() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nueva cita");
        alert.setHeaderText(null);
        alert.setContentText("Aquí se abriría el formulario de nueva cita.");
        alert.showAndWait();
    }

    @FXML
    private void onRefrescar() {
        loadCitas();
    }

    @FXML
    private void onAplicarFiltros() {
        loadCitas();
    }

    @FXML
    private void onLimpiarFiltros() {
        txtFiltroTexto.clear();
        cbFiltroEstado.getSelectionModel().selectFirst();
        dpFiltroDesde.setValue(null);
        dpFiltroHasta.setValue(null);
        loadCitas();
    }

    @FXML
    private void onAgendaHoy() {
        dpAgendaFecha.setValue(LocalDate.now());
        loadAgenda();
    }

    private void loadCitas() {
        Long empresaId = sessionManager.getCurrentEmpresaId();
        if (empresaId == null) {
            showError("No se ha encontrado la empresa actual en la sesión.");
            return;
        }

        List<Appointment> lista = listAppointmentsUseCase.execute(empresaId);
        citas.setAll(lista);
        loadAgenda();
    }

    private void loadAgenda() {
        LocalDate selectedDate = dpAgendaFecha.getValue();
        if (selectedDate == null) {
            lstAgenda.getItems().clear();
            return;
        }

        var filtered = citas.filtered(c -> c.getDateTime() != null && c.getDateTime().toLocalDate().isEqual(selectedDate));
        lstAgenda.setItems(filtered);
    }

    private void onEditarCita(Appointment appointment) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Editar cita");
        alert.setHeaderText(null);
        alert.setContentText("Aquí se abriría el formulario para editar la cita ID " + appointment.getId());
        alert.showAndWait();
    }

    private void onCompletarCita(Appointment appointment) {
        Long empresaId = sessionManager.getCurrentEmpresaId();
        if (empresaId == null) {
            showError("No se ha encontrado la empresa actual en la sesión.");
            return;
        }

        try {
            changeAppointmentStatusUseCase.execute(appointment.getId(), empresaId, AppointmentStatus.COMPLETED);
            loadCitas();
        } catch (Exception e) {
            showError("Error al marcar cita como completada: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String appointmentValueOrBlank(Long value) {
        return value != null ? String.valueOf(value) : "";
    }

    private String mapStatusToLabel(AppointmentStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente";
            case CONFIRMED -> "Confirmada";
            case CANCELLED -> "Cancelada";
            case COMPLETED -> "Completada";
        };
    }

    private String mapOriginToLabel(AppointmentOrigin origin) {
        return switch (origin) {
            case INTERNAL -> "Interno";
            case TELEGRAM -> "Telegram";
        };
    }
}
