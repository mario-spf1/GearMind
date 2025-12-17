package com.gearmind.presentation.controller;

import com.gearmind.application.appointment.SaveAppointmentRequest;
import com.gearmind.application.appointment.SaveAppointmentUseCase;
import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentOrigin;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRepository;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class CitaFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private DatePicker dpFecha;
    @FXML
    private TextField txtHora;
    @FXML
    private ComboBox<CustomerOption> cbCliente;
    @FXML
    private TextField txtVehiculoId;
    @FXML
    private ComboBox<EmployeeOption> cbEmpleado;
    @FXML
    private TextArea txtNotas;

    private Long empresaId;
    private SaveAppointmentUseCase saveAppointmentUseCase;
    private Appointment existingAppointment;
    private boolean saved = false;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final ObservableList<CustomerOption> allCustomers = FXCollections.observableArrayList();
    private final ObservableList<EmployeeOption> allEmployees = FXCollections.observableArrayList();
    private boolean settingClienteProgrammatically = false;
    private boolean settingEmpleadoProgrammatically = false;

    public void init(Long empresaId, SaveAppointmentUseCase saveAppointmentUseCase, Appointment existingAppointment) {
        this.empresaId = empresaId;
        this.saveAppointmentUseCase = saveAppointmentUseCase;
        this.existingAppointment = existingAppointment;
        configureCombos();

        if (existingAppointment != null) {
            lblTitulo.setText("Editar cita");

            LocalDateTime dt = existingAppointment.getDateTime();
            if (dt != null) {
                dpFecha.setValue(dt.toLocalDate());
                txtHora.setText(dt.toLocalTime().format(timeFormatter));
            }

            if (existingAppointment.getVehicleId() != null) {
                txtVehiculoId.setText(String.valueOf(existingAppointment.getVehicleId()));
            }

            if (existingAppointment.getNotes() != null) {
                txtNotas.setText(existingAppointment.getNotes());
            }

            if (existingAppointment.getCustomerId() != null) {
                selectCustomerById(existingAppointment.getCustomerId());
            }

            if (existingAppointment.getEmployeeId() != null) {
                selectEmployeeById(existingAppointment.getEmployeeId());
            }
        } else {
            lblTitulo.setText("Nueva cita");
            dpFecha.setValue(LocalDate.now());
        }
    }

    private void configureCombos() {
        CustomerRepository customerRepository = new MySqlCustomerRepository();
        List<Customer> customers = customerRepository.findByEmpresaId(empresaId);

        allCustomers.clear();
        for (Customer c : customers) {
            String label = c.getNombre() + " (ID " + c.getId() + ")";
            allCustomers.add(new CustomerOption(c.getId(), label));
        }

        FilteredList<CustomerOption> filteredCustomers = new FilteredList<>(allCustomers, opt -> true);
        cbCliente.setItems(filteredCustomers);
        cbCliente.setEditable(true);

        cbCliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(CustomerOption object) {
                return object == null ? "" : object.getLabel();
            }

            @Override
            public CustomerOption fromString(String string) {
                if (string == null) {
                    return null;
                }
                String s = string.trim().toLowerCase(Locale.ROOT);
                if (s.isBlank()) {
                    return null;
                }
                return allCustomers.stream().filter(o -> o.getLabel().toLowerCase(Locale.ROOT).equals(s)).findFirst().orElse(null);
            }
        });

        cbCliente.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            if (settingClienteProgrammatically) {
                return;
            }

            CustomerOption selected = cbCliente.getSelectionModel().getSelectedItem();
            String nt = (newV == null ? "" : newV).trim();

            if (selected != null && selected.getLabel() != null
                    && selected.getLabel().equalsIgnoreCase(nt)) {
                return;
            }

            String filtro = nt.toLowerCase(Locale.ROOT);
            settingClienteProgrammatically = true;
            try {
                filteredCustomers.setPredicate(opt -> filtro.isBlank() || opt.getLabel().toLowerCase(Locale.ROOT).contains(filtro));
            } finally {
                settingClienteProgrammatically = false;
            }

            if (cbCliente.isFocused() && !cbCliente.isShowing()) {
                cbCliente.show();
            }
        });

        cbCliente.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            settingClienteProgrammatically = true;
            try {
                cbCliente.getEditor().setText(newVal.getLabel());
            } finally {
                settingClienteProgrammatically = false;
            }
        });

        cbCliente.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(CustomerOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getLabel());
            }
        });
        cbCliente.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(CustomerOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getLabel());
            }
        });

        UserRepository userRepository = new MySqlUserRepository();
        List<User> users = userRepository.findByEmpresaId(empresaId);
        allEmployees.clear();
        for (User u : users) {
            String label = u.getNombre() + " (ID " + u.getId() + ")";
            allEmployees.add(new EmployeeOption(u.getId(), label, u.getRol()));
        }

        FilteredList<EmployeeOption> filteredEmployees = new FilteredList<>(allEmployees, opt -> true);
        cbEmpleado.setItems(filteredEmployees);
        cbEmpleado.setEditable(true);
        cbEmpleado.setConverter(new StringConverter<>() {
            @Override
            public String toString(EmployeeOption object) {
                return object == null ? "" : object.getLabel();
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

                return allEmployees.stream().filter(o -> o.getLabel().toLowerCase(Locale.ROOT).equals(s)).findFirst().orElse(null);
            }
        });

        cbEmpleado.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            if (settingEmpleadoProgrammatically) {
                return;
            }

            EmployeeOption selected = cbEmpleado.getSelectionModel().getSelectedItem();
            String nt = (newV == null ? "" : newV).trim();

            if (selected != null && selected.getLabel() != null
                    && selected.getLabel().equalsIgnoreCase(nt)) {
                return;
            }

            String filtro = nt.toLowerCase(Locale.ROOT);
            settingEmpleadoProgrammatically = true;
            try {
                filteredEmployees.setPredicate(opt -> filtro.isBlank() || opt.getLabel().toLowerCase(Locale.ROOT).contains(filtro));
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
                cbEmpleado.getEditor().setText(newVal.getLabel());
            } finally {
                settingEmpleadoProgrammatically = false;
            }
        });

        cbEmpleado.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(EmployeeOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getLabel());
            }
        });
        cbEmpleado.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(EmployeeOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getLabel());
            }
        });

        if (AuthContext.isLoggedIn() && AuthContext.getRole() == UserRole.EMPLEADO) {
            User current = AuthContext.getCurrentUser();
            if (current != null) {
                EmployeeOption self = allEmployees.stream().filter(e -> e.getId().equals(current.getId())).findFirst().orElse(new EmployeeOption(current.getId(), current.getNombre() + " (ID " + current.getId() + ")", current.getRol()));

                if (allEmployees.stream().noneMatch(e -> e.getId().equals(self.getId()))) {
                    allEmployees.add(self);
                }
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
    }

    private void selectCustomerById(Long customerId) {
        allCustomers.stream().filter(o -> o.getId().equals(customerId)).findFirst().ifPresent(opt -> {
            settingClienteProgrammatically = true;
            try {
                cbCliente.setValue(opt);
            } finally {
                settingClienteProgrammatically = false;
            }
        });
    }

    private void selectEmployeeById(Long employeeId) {
        allEmployees.stream().filter(o -> o.getId().equals(employeeId)).findFirst().ifPresent(opt -> {
            settingEmpleadoProgrammatically = true;
            try {
                cbEmpleado.setValue(opt);
            } finally {
                settingEmpleadoProgrammatically = false;
            }
        });
    }

    @FXML
    private void onCancelar() {
        close();
    }

    @FXML
    private void onGuardar() {
        try {
            SaveAppointmentRequest request = buildRequest();
            saveAppointmentUseCase.execute(request);
            saved = true;
            close();
        } catch (Exception e) {
            showError("Error al guardar la cita: " + e.getMessage());
        }
    }

    private SaveAppointmentRequest buildRequest() {
        if (empresaId == null) {
            throw new IllegalArgumentException("No se ha establecido la empresa para la cita.");
        }

        CustomerOption clienteOpt = cbCliente.getValue();
        if (clienteOpt == null) {
            throw new IllegalArgumentException("Debes seleccionar un cliente.");
        }
        Long clienteId = clienteOpt.getId();
        Long vehiculoId = null;
        String vehiculoStr = txtVehiculoId.getText();
        if (vehiculoStr != null && !vehiculoStr.isBlank()) {
            try {
                vehiculoId = Long.parseLong(vehiculoStr.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El ID de vehículo debe ser numérico.");
            }
        }

        LocalDate fecha = dpFecha.getValue();
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }

        String horaStr = txtHora.getText();
        if (horaStr == null || horaStr.isBlank()) {
            throw new IllegalArgumentException("La hora es obligatoria (formato HH:mm).");
        }

        LocalTime hora;
        try {
            hora = LocalTime.parse(horaStr.trim(), timeFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La hora debe tener formato HH:mm.");
        }

        LocalDateTime dateTime = LocalDateTime.of(fecha, hora);

        SaveAppointmentRequest request = new SaveAppointmentRequest();

        if (existingAppointment != null) {
            request.setId(existingAppointment.getId());
        }

        request.setEmpresaId(empresaId);
        request.setCustomerId(clienteId);
        request.setVehicleId(vehiculoId);
        request.setDateTime(dateTime);
        request.setNotes(txtNotas.getText());

        if (existingAppointment != null) {
            request.setOrigin(existingAppointment.getOrigin());
            request.setStatus(existingAppointment.getStatus());
        } else {
            request.setOrigin(AppointmentOrigin.INTERNAL);
        }

        Long currentUserId = null;
        UserRole currentUserRole = null;
        if (AuthContext.isLoggedIn()) {
            User current = AuthContext.getCurrentUser();
            if (current != null) {
                currentUserId = current.getId();
            }
            currentUserRole = AuthContext.getRole();
        }

        Long employeeId = null;
        EmployeeOption empOpt = cbEmpleado.getValue();
        if (empOpt != null) {
            employeeId = empOpt.getId();
        }

        request.setEmployeeId(employeeId);
        request.setCurrentUserId(currentUserId);
        request.setCurrentUserRole(currentUserRole);
        return request;
    }

    private void close() {
        Stage stage = (Stage) lblTitulo.getScene().getWindow();
        stage.close();
    }

    public boolean isSaved() {
        return saved;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class CustomerOption {

        private final Long id;
        private final String label;

        public CustomerOption(Long id, String label) {
            this.id = id;
            this.label = label;
        }

        public Long getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static class EmployeeOption {

        private final Long id;
        private final String label;
        private final UserRole role;

        public EmployeeOption(Long id, String label, UserRole role) {
            this.id = id;
            this.label = label;
            this.role = role;
        }

        public Long getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public UserRole getRole() {
            return role;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
