package com.gearmind.presentation.controller;

import com.gearmind.application.appointment.ChangeAppointmentStatusUseCase;
import com.gearmind.application.appointment.ListAppointmentsUseCase;
import com.gearmind.application.appointment.SaveAppointmentUseCase;
import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.message.SendMessageUseCase;
import com.gearmind.application.telegram.ListTelegramAppointmentRequestsUseCase;
import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentOrigin;
import com.gearmind.domain.appointment.AppointmentStatus;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.telegram.TelegramAppointmentRequest;
import com.gearmind.domain.telegram.TelegramAppointmentRequestStatus;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import com.gearmind.infrastructure.appointment.MySqlAppointmentRepository;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import com.gearmind.infrastructure.database.DataSourceFactory;
import com.gearmind.infrastructure.message.MySqlMessageRepository;
import com.gearmind.infrastructure.vehicle.MySqlVehicleRepository;
import com.gearmind.presentation.table.SmartTable;
import com.gearmind.application.telegram.SendTelegramNotificationUseCase;
import com.gearmind.application.telegram.UpdateTelegramAppointmentRequestStatusUseCase;
import com.gearmind.infrastructure.telegram.MySqlTelegramRepository;
import com.gearmind.infrastructure.telegram.TelegramBotClient;
import com.gearmind.infrastructure.telegram.TelegramConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.layout.VBox;

public class CitasController {

    @FXML
    private TabPane tabPane;
    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Button btnSolicitudesBot;
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
    private TextField filterNotasField;
    @FXML
    private ComboBox<EmpresaOption> cbFiltroEmpresa;
    @FXML
    private TextField filterEmpresaField;
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
    private final ListTelegramAppointmentRequestsUseCase listTelegramAppointmentRequestsUseCase;
    private final UpdateTelegramAppointmentRequestStatusUseCase updateTelegramAppointmentRequestStatusUseCase;
    private SmartTable<Appointment> smartTable;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());
    private final DataSource dataSource = DataSourceFactory.getDataSource();
    private final ObservableList<EmpresaOption> empresas = FXCollections.observableArrayList();
    private final Map<Long, String> empresaNameById = new HashMap<>();
    private final Map<Long, String> customerNameById = new HashMap<>();
    private final Map<Long, String> employeeNameById = new HashMap<>();
    private final Map<Long, String> vehicleLabelById = new HashMap<>();
    private boolean settingEmpresaComboProgrammatically = false;
    private final Pattern requestedDatePattern = Pattern.compile("Fecha solicitada: (\\d{4}-\\d{2}-\\d{2}) (\\d{2}):(\\d{2})");

    public CitasController() {
        MySqlAppointmentRepository repo = new MySqlAppointmentRepository();
        this.listAppointmentsUseCase = new ListAppointmentsUseCase(repo);
        TelegramConfig telegramConfig = new TelegramConfig();
        TelegramBotClient botClient = new TelegramBotClient(telegramConfig, new ObjectMapper());
        MySqlTelegramRepository telegramRepository = new MySqlTelegramRepository();
        SendTelegramNotificationUseCase notificationUseCase = new SendTelegramNotificationUseCase(telegramConfig, botClient, telegramRepository, telegramRepository);
        SendMessageUseCase internalMessageUseCase = new SendMessageUseCase(new MySqlMessageRepository());
        this.saveAppointmentUseCase = new SaveAppointmentUseCase(repo, notificationUseCase, internalMessageUseCase);
        this.changeAppointmentStatusUseCase = new ChangeAppointmentStatusUseCase(repo, notificationUseCase);
        this.listTelegramAppointmentRequestsUseCase = new ListTelegramAppointmentRequestsUseCase(telegramRepository);
        this.updateTelegramAppointmentRequestStatusUseCase = new UpdateTelegramAppointmentRequestStatusUseCase(telegramRepository);

    }

    @FXML
    private void initialize() {
        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(10, 25, 50, 100));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(25));
        }

        configureAgendaControls();
        configureAgendaList();
        configureTable();
        tblCitas.setFixedCellSize(28);
        smartTable = new SmartTable<>(tblCitas, masterData, null, cmbPageSize, lblResumen, "citas", this::matchesGlobalFilter);

        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblCitas.getFixedCellSize() + 2;

            tblCitas.setPrefHeight(tableHeight);
            tblCitas.setMinHeight(Region.USE_PREF_SIZE);
            tblCitas.setMaxHeight(Region.USE_PREF_SIZE);
        });

        smartTable.addColumnFilter(filterClienteField, (cita, text) -> safe(getCustomerLabel(cita.getCustomerId())).contains(text));
        smartTable.addColumnFilter(filterVehiculoField, (cita, text) -> safe(getVehicleLabel(cita.getVehicleId())).contains(text));
        smartTable.addColumnFilter(filterEmpleadoField, (cita, text) -> safe(getEmployeeLabel(cita.getEmployeeId())).contains(text));
        smartTable.addColumnFilter(filterEmpresaField, (cita, text) -> safe(getEmpresaName(cita.getEmpresaId())).contains(text));
        smartTable.addColumnFilter(filterNotasField, (cita, text) -> safe(cita.getNotes()).contains(text));
        smartTable.addColumnFilter(filterEstadoField, (cita, text) -> safe(mapStatusToLabel(cita.getStatus())).contains(text));
        smartTable.addColumnFilter(filterOrigenField, (cita, text) -> safe(mapOriginToLabel(cita.getOrigin())).contains(text));

        configureEstadoYOrigenCombos();
        dpAgendaFecha.setValue(LocalDate.now());
        applyRoleVisibility();
        configureEmpresaComboIfSuperAdmin();
        reloadFromDb();
        refreshPendingRequestCount();
        if (tabPane != null) {
            tabPane.getSelectionModel().select(0);
        }
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
                    Label libre = new Label("Sin citas");
                    libre.getStyleClass().add("tfx-muted");
                    citasBox.getChildren().add(libre);
                } else {
                    for (Appointment a : item.appointments()) {
                        String horaReal = a.getDateTime().toLocalTime().format(timeFormatter);
                        String cliente = getCustomerLabel(a.getCustomerId());
                        String empleado = getEmployeeLabel(a.getEmployeeId());
                        String vehiculo = getVehicleLabel(a.getVehicleId());
                        String texto = horaReal + " · " + cliente;
                        if (!vehiculo.isBlank()) {
                            texto += " · " + vehiculo;
                        }
                        if (!empleado.isBlank()) {
                            texto += " · " + empleado;
                        }
                        Button chip = new Button(texto);
                        chip.getStyleClass().add("tfx-agenda-chip");
                        String statusClass = mapStatusToChipClass(a.getStatus());
                        if (statusClass != null) {
                            chip.getStyleClass().add(statusClass);
                        }
                        chip.setOnAction(e -> openCitaForm(a));
                        citasBox.getChildren().add(chip);
                    }
                }

                HBox row = new HBox(12, lblHora, citasBox);
                row.getStyleClass().add("tfx-agenda-row");
                row.setFillHeight(true);
                setText(null);
                setGraphic(row);
            }
        });
    }

    private void configureTable() {
        tblCitas.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tblCitas.setItems(masterData);
        configureColumnWidths();

        colFechaHora.setCellValueFactory(cellData -> {
            Appointment appointment = cellData.getValue();
            if (appointment.getDateTime() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(appointment.getDateTime().format(dateTimeFormatter));
        });

        colCliente.setCellValueFactory(cellData -> new SimpleStringProperty(getCustomerLabel(cellData.getValue().getCustomerId())));
        colVehiculo.setCellValueFactory(cellData -> new SimpleStringProperty(getVehicleLabel(cellData.getValue().getVehicleId())));
        colEmpleado.setCellValueFactory(cellData -> new SimpleStringProperty(getEmployeeLabel(cellData.getValue().getEmployeeId())));
        colEmpresa.setCellValueFactory(cellData -> new SimpleStringProperty(getEmpresaName(cellData.getValue().getEmpresaId())));
        colEstado.setCellValueFactory(cellData -> {
            AppointmentStatus status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status != null ? mapStatusToLabel(status) : "");
        });
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-table-badge", "tfx-badge-success", "tfx-badge-warning", "tfx-badge-danger");
                if (empty || item == null) {
                    setText(null);
                    setAlignment(javafx.geometry.Pos.CENTER);
                    return;
                }
                setText(item);
                getStyleClass().addAll("tfx-badge", "tfx-table-badge");
                switch (item) {
                    case "Completada" ->
                        getStyleClass().add("tfx-badge-success");
                    case "Cancelada" ->
                        getStyleClass().add("tfx-badge-danger");
                    case "Confirmada", "Solicitada" ->
                        getStyleClass().add("tfx-badge-warning");
                    default -> {
                    }
                }
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        colOrigen.setCellValueFactory(cellData -> {
            AppointmentOrigin origin = cellData.getValue().getOrigin();
            return new SimpleStringProperty(origin != null ? mapOriginToLabel(origin) : "");
        });

        colNotas.setCellValueFactory(cellData -> {
            String notas = cellData.getValue().getNotes();
            return new SimpleStringProperty(notas == null ? "" : notas);
        });

        colNotas.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(item);
                setTooltip(new Tooltip(item));
            }
        });

        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnCompletar = new Button("Completar");
            private final Button btnCancelar = new Button("Cancelar");
            private final HBox box = new HBox(8, btnEditar, btnCompletar, btnCancelar);

            {
                box.getStyleClass().add("tfx-table-actions");
                btnEditar.getStyleClass().add("tfx-table-action-btn");
                btnCompletar.getStyleClass().addAll("tfx-table-action-btn", "tfx-table-action-success");
                btnCancelar.getStyleClass().addAll("tfx-table-action-btn", "tfx-table-action-danger");
                btnEditar.setTooltip(new Tooltip("Editar cita"));
                btnCompletar.setTooltip(new Tooltip("Marcar como completada"));
                btnCancelar.setTooltip(new Tooltip("Cancelar cita"));

                btnEditar.setOnAction(e -> {
                    Appointment a = (getTableRow() != null) ? getTableRow().getItem() : null;
                    if (a != null) {
                        openCitaForm(a);
                    }
                });

                btnCompletar.setOnAction(e -> {
                    Appointment a = (getTableRow() != null) ? getTableRow().getItem() : null;
                    if (a != null) {
                        completarCita(a);
                    }
                });

                btnCancelar.setOnAction(e -> {
                    Appointment a = (getTableRow() != null) ? getTableRow().getItem() : null;
                    if (a != null) {
                        cancelarCita(a);
                    }
                });
            }

            @Override
            protected void updateItem(Appointment appointment, boolean empty) {
                super.updateItem(appointment, empty);
                if (empty || appointment == null) {
                    setGraphic(null);
                    return;
                }
                boolean disabled = appointment.getStatus() == AppointmentStatus.CANCELLED || appointment.getStatus() == AppointmentStatus.COMPLETED;
                btnCompletar.setDisable(disabled);
                btnCancelar.setDisable(disabled);
                setGraphic(box);
            }
        });
    }

    private void configureColumnWidths() {
        if (colFechaHora != null) {
            colFechaHora.setPrefWidth(130);
            colFechaHora.setMinWidth(120);
        }
        if (colCliente != null) {
            colCliente.setPrefWidth(180);
            colCliente.setMinWidth(160);
        }
        if (colVehiculo != null) {
            colVehiculo.setPrefWidth(190);
            colVehiculo.setMinWidth(170);
        }
        if (colEmpleado != null) {
            colEmpleado.setPrefWidth(150);
            colEmpleado.setMinWidth(130);
        }
        if (colEmpresa != null) {
            colEmpresa.setPrefWidth(120);
            colEmpresa.setMinWidth(110);
        }
        if (colEstado != null) {
            colEstado.setPrefWidth(110);
            colEstado.setMinWidth(100);
        }
        if (colOrigen != null) {
            colOrigen.setPrefWidth(100);
            colOrigen.setMinWidth(90);
        }
        if (colNotas != null) {
            colNotas.setPrefWidth(180);
            colNotas.setMinWidth(150);
        }
        if (colAcciones != null) {
            colAcciones.setPrefWidth(210);
            colAcciones.setMinWidth(200);
        }
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

    private void configureEmpresaComboIfSuperAdmin() {
        if (cbFiltroEmpresa == null) {
            return;
        }

        if (!AuthContext.isLoggedIn() || AuthContext.getRole() != UserRole.SUPER_ADMIN) {
            cbFiltroEmpresa.setVisible(false);
            cbFiltroEmpresa.setManaged(false);
            return;
        }

        empresas.clear();
        empresaNameById.clear();
        empresas.add(new EmpresaOption(0L, "Todas"));

        String sql = "SELECT id, nombre FROM empresa ORDER BY nombre ASC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("id");
                String nombre = rs.getString("nombre");
                empresas.add(new EmpresaOption(id, nombre));
                empresaNameById.put(id, nombre);
            }

        } catch (Exception ex) {
            empresas.clear();
            empresas.add(new EmpresaOption(0L, "Todas"));
            empresaNameById.clear();
        }

        cbFiltroEmpresa.setItems(empresas);
        cbFiltroEmpresa.getSelectionModel().selectFirst();

        cbFiltroEmpresa.valueProperty().addListener((obs, oldV, newV) -> {
            if (settingEmpresaComboProgrammatically) {
                return;
            }

            if (newV == null || newV.id == 0L) {
                filterEmpresaField.setText("");
            } else {
                filterEmpresaField.setText(safe(newV.nombre));
            }
            smartTable.refresh();
        });
    }

    private void applyRoleVisibility() {
        if (!AuthContext.isLoggedIn()) {
            colEmpresa.setVisible(false);
            return;
        }

        UserRole role = AuthContext.getRole();
        if (role != UserRole.SUPER_ADMIN) {
            if (colEmpresa != null) {
                colEmpresa.setVisible(false);
            }

            if (cbFiltroEmpresa != null) {
                cbFiltroEmpresa.setVisible(false);
                cbFiltroEmpresa.setManaged(false);
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
    private void onSolicitudesBot() {
        openTelegramRequestsDialog();
    }

    @FXML
    private void onAgendaHoy() {
        dpAgendaFecha.setValue(LocalDate.now());
        loadAgenda();
    }

    @FXML
    private void onLimpiarFiltros() {
        if (filterClienteField != null) {
            filterClienteField.clear();
        }
        if (filterVehiculoField != null) {
            filterVehiculoField.clear();
        }
        if (filterEmpleadoField != null) {
            filterEmpleadoField.clear();
        }
        if (filterNotasField != null) {
            filterNotasField.clear();
        }

        if (cbFiltroEstado != null) {
            cbFiltroEstado.getSelectionModel().selectFirst();
        }
        if (cbFiltroOrigen != null) {
            cbFiltroOrigen.getSelectionModel().selectFirst();
        }

        if (filterEstadoField != null) {
            filterEstadoField.clear();
        }
        if (filterOrigenField != null) {
            filterOrigenField.clear();
        }

        if (cbFiltroEmpresa != null && cbFiltroEmpresa.isManaged()) {
            cbFiltroEmpresa.getSelectionModel().selectFirst();
        }
        if (filterEmpresaField != null) {
            filterEmpresaField.clear();
        }

        if (smartTable != null) {
            smartTable.refresh();
        }
        loadAgenda();
    }

    private void reloadFromDb() {
        long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
        loadLookupMaps(empresaId);
        List<Appointment> citas = new ArrayList<>();
        for (Appointment cita : listAppointmentsUseCase.execute(empresaId)) {
            if (cita != null && cita.getStatus() == AppointmentStatus.REQUESTED) {
                continue;
            }
            citas.add(cita);
        }

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
        refreshPendingRequestCount();
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
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setScene(scene);
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

    private void cancelarCita(Appointment appointment) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancelar cita");
        confirm.setHeaderText("¿Cancelar esta cita?");
        confirm.setContentText("Se notificará automáticamente al cliente.");
        Optional<ButtonType> response = confirm.showAndWait();
        if (response.isEmpty() || response.get() != ButtonType.OK) {
            return;
        }

        try {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            changeAppointmentStatusUseCase.execute(appointment.getId(), empresaId, AppointmentStatus.CANCELLED);
            reloadFromDb();
        } catch (Exception e) {
            showError("Error al cancelar la cita: " + e.getMessage());
        }
    }

    private void openTelegramRequestsDialog() {
        long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
        List<TelegramAppointmentRequest> requests = listTelegramAppointmentRequestsUseCase.execute(empresaId, TelegramAppointmentRequestStatus.PENDIENTE);

        if (requests.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Solicitudes Telegram");
            alert.setHeaderText(null);
            alert.setContentText("No hay solicitudes pendientes.");
            alert.showAndWait();
            return;
        }

        TableView<TelegramRequestRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        ObservableList<TelegramRequestRow> rows = FXCollections.observableArrayList();
        for (TelegramAppointmentRequest request : requests) {
            LocalDateTime requestedDateTime = parseRequestedDateTime(request.getMensaje());
            String cliente = getCustomerLabel(request.getClienteId());
            String vehiculo = getVehicleLabel(request.getVehiculoId());
            rows.add(new TelegramRequestRow(request, requestedDateTime, cliente, vehiculo));
        }
        table.setItems(rows);

        TableColumn<TelegramRequestRow, String> colFecha = new TableColumn<>("Solicitud");
        colFecha.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().fechaSolicitud()));

        TableColumn<TelegramRequestRow, String> colFechaSolicitada = new TableColumn<>("Fecha solicitada");
        colFechaSolicitada.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().fechaSolicitada()));

        TableColumn<TelegramRequestRow, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().cliente()));

        TableColumn<TelegramRequestRow, String> colVehiculo = new TableColumn<>("Vehículo");
        colVehiculo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().vehiculo()));

        TableColumn<TelegramRequestRow, String> colMensaje = new TableColumn<>("Mensaje");
        colMensaje.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().mensaje()));
        colMensaje.setCellFactory(column -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setWrapText(true);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                label.setText(item);
                setGraphic(label);
            }
        });

        TableColumn<TelegramRequestRow, TelegramRequestRow> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setMinWidth(170);
        colAcciones.setPrefWidth(190);
        colAcciones.setCellFactory(column -> new TableCell<>() {
            private final Button btnAceptar = new Button("Aceptar");
            private final Button btnRechazar = new Button("Rechazar");
            private final HBox box = new HBox(8, btnAceptar, btnRechazar);

            {
                btnAceptar.getStyleClass().addAll("tfx-icon-btn", "tfx-icon-btn-success");
                btnRechazar.getStyleClass().addAll("tfx-icon-btn", "tfx-icon-btn-danger");

                btnAceptar.setOnAction(e -> {
                    TelegramRequestRow row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row != null) {
                        aceptarSolicitud(row, rows);
                    }
                });
                btnRechazar.setOnAction(e -> {
                    TelegramRequestRow row = getTableRow() != null ? getTableRow().getItem() : null;
                    if (row != null) {
                        rechazarSolicitud(row, rows);
                    }
                });
            }

            @Override
            protected void updateItem(TelegramRequestRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(box);
            }
        });

        table.getColumns().addAll(List.of(colFecha, colFechaSolicitada, colCliente, colVehiculo, colMensaje, colAcciones));

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.getStyleClass().add("button-secondary");

        HBox footer = new HBox(8, btnCerrar);
        footer.setStyle("-fx-alignment: center-right;");

        VBox root = new VBox(12, table, footer);
        root.setPadding(new javafx.geometry.Insets(12));
        root.getStyleClass().add("tfx-card");
        root.setMinWidth(1000);

        Scene scene = new Scene(root, 1100, 520);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Solicitudes de cita (Telegram)");
        stage.setScene(scene);
        btnCerrar.setOnAction(e -> stage.close());
        stage.showAndWait();
        refreshPendingRequestCount();
    }

    private String getEmpresaName(Long empresaId) {
        if (empresaId == null) {
            return "";
        }
        String n = empresaNameById.get(empresaId);
        return n != null ? n : String.valueOf(empresaId);
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

    private String getCustomerLabel(Long customerId) {
        if (customerId == null) {
            return "";
        }
        String label = customerNameById.get(customerId);
        return label != null ? label : "Cliente " + customerId;
    }

    private String getEmployeeLabel(Long employeeId) {
        if (employeeId == null) {
            return "";
        }
        String label = employeeNameById.get(employeeId);
        return label != null ? label : "Empleado " + employeeId;
    }

    private String getVehicleLabel(Long vehicleId) {
        if (vehicleId == null) {
            return "";
        }
        String label = vehicleLabelById.get(vehicleId);
        return label != null ? label : "Vehículo " + vehicleId;
    }

    private String mapStatusToChipClass(AppointmentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case REQUESTED ->
                "tfx-chip-requested";
            case CONFIRMED ->
                "tfx-chip-confirmed";
            case CANCELLED ->
                "tfx-chip-cancelled";
            case COMPLETED ->
                "tfx-chip-completed";
        };
    }

    private void loadLookupMaps(long empresaId) {
        customerNameById.clear();
        employeeNameById.clear();
        vehicleLabelById.clear();

        try {
            MySqlCustomerRepository customerRepository = new MySqlCustomerRepository();
            for (Customer customer : customerRepository.findByEmpresaId(empresaId)) {
                customerNameById.put(customer.getId(), formatCustomerLabel(customer));
            }
        } catch (Exception ignored) {
        }

        try {
            MySqlUserRepository userRepository = new MySqlUserRepository();
            for (User user : userRepository.findByEmpresaId(empresaId)) {
                employeeNameById.put(user.getId(), formatEmployeeLabel(user));
            }
        } catch (Exception ignored) {
        }

        try {
            MySqlVehicleRepository vehicleRepository = new MySqlVehicleRepository();
            for (Vehicle vehicle : vehicleRepository.findByEmpresaId(empresaId)) {
                vehicleLabelById.put(vehicle.getId(), formatVehicleLabel(vehicle));
            }
        } catch (Exception ignored) {
        }
    }

    private String formatCustomerLabel(Customer customer) {
        if (customer == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (customer.getNombre() != null) {
            sb.append(customer.getNombre());
        }
        if (customer.getDni() != null && !customer.getDni().isBlank()) {
            sb.append(" · ").append(customer.getDni());
        }
        sb.append(" (ID ").append(customer.getId()).append(")");
        return sb.toString();
    }

    private String formatEmployeeLabel(User user) {
        if (user == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (user.getNombre() != null) {
            sb.append(user.getNombre());
        }
        sb.append(" (ID ").append(user.getId()).append(")");
        return sb.toString();
    }

    private String formatVehicleLabel(Vehicle vehicle) {
        if (vehicle == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (vehicle.getMatricula() != null && !vehicle.getMatricula().isBlank()) {
            sb.append(vehicle.getMatricula());
        }
        if (vehicle.getMarca() != null && !vehicle.getMarca().isBlank()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(vehicle.getMarca());
        }
        if (vehicle.getModelo() != null && !vehicle.getModelo().isBlank()) {
            sb.append(" ").append(vehicle.getModelo());
        }
        if (sb.length() == 0) {
            sb.append("Vehículo ").append(vehicle.getId());
        } else {
            sb.append(" (ID ").append(vehicle.getId()).append(")");
        }
        return sb.toString();
    }

    private void refreshPendingRequestCount() {
        if (btnSolicitudesBot == null) {
            return;
        }
        try {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            int count = listTelegramAppointmentRequestsUseCase.execute(empresaId, TelegramAppointmentRequestStatus.PENDIENTE).size();
            String suffix = count > 0 ? " (" + count + ")" : "";
            btnSolicitudesBot.setText("Solicitudes bot" + suffix);
        } catch (Exception e) {
            btnSolicitudesBot.setText("Solicitudes bot");
        }
    }

    private LocalDateTime parseRequestedDateTime(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return null;
        }
        Matcher matcher = requestedDatePattern.matcher(mensaje);
        if (!matcher.find()) {
            return null;
        }
        String datePart = matcher.group(1);
        String hourPart = matcher.group(2);
        String minutePart = matcher.group(3);
        try {
            return LocalDateTime.parse(datePart + " " + hourPart + ":" + minutePart, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            return null;
        }
    }

    private void aceptarSolicitud(TelegramRequestRow row, ObservableList<TelegramRequestRow> rows) {
        if (row == null || row.request() == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CitaFormView.fxml"));
            Parent root = loader.load();
            CitaFormController controller = loader.getController();
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            controller.init(empresaId, saveAppointmentUseCase, null);
            controller.applyTelegramRequestPrefill(row.request(), row.requestedDateTime());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Confirmar cita solicitada");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            if (controller.isSaved()) {
                updateTelegramAppointmentRequestStatusUseCase.execute(row.request().getId(), TelegramAppointmentRequestStatus.PROCESADA);
                rows.remove(row);
                reloadFromDb();
            }
        } catch (IOException e) {
            showError("No se ha podido abrir el formulario de cita: " + e.getMessage());
        } catch (Exception e) {
            showError("No se pudo confirmar la solicitud: " + e.getMessage());
        }
    }

    private void rechazarSolicitud(TelegramRequestRow row, ObservableList<TelegramRequestRow> rows) {
        if (row == null || row.request() == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Rechazar solicitud");
        confirm.setHeaderText("¿Rechazar esta solicitud de cita?");
        confirm.setContentText("Se marcará como descartada.");
        Optional<ButtonType> response = confirm.showAndWait();
        if (response.isEmpty() || response.get() != ButtonType.OK) {
            return;
        }

        try {
            updateTelegramAppointmentRequestStatusUseCase.execute(row.request().getId(), TelegramAppointmentRequestStatus.DESCARTADA);
            rows.remove(row);
            refreshPendingRequestCount();
        } catch (Exception e) {
            showError("No se pudo rechazar la solicitud: " + e.getMessage());
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private boolean matchesGlobalFilter(Appointment c, String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String t = safe(text);

        String haystack
                = (safe(getCustomerLabel(c.getCustomerId())) + " "
                + safe(getVehicleLabel(c.getVehicleId())) + " "
                + safe(getEmployeeLabel(c.getEmployeeId())) + " "
                + safe(getEmpresaName(c.getEmpresaId())) + " "
                + safe(c.getNotes()) + " "
                + safe(mapStatusToLabel(c.getStatus())) + " "
                + safe(mapOriginToLabel(c.getOrigin())));

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

    private record TelegramRequestRow(TelegramAppointmentRequest request, LocalDateTime requestedDateTime, String cliente, String vehiculo) {

        String fechaSolicitud() {
            if (request == null || request.getFecha() == null) {
                return "";
            }
            return request.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        String fechaSolicitada() {
            if (requestedDateTime == null) {
                return "Por confirmar";
            }
            return requestedDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        String mensaje() {
            return request != null && request.getMensaje() != null ? request.getMensaje() : "";
        }
    }

    private static class EmpresaOption {

        final long id;
        final String nombre;

        EmpresaOption(long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }
}
