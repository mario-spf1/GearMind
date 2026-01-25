package com.gearmind.presentation.controller;

import com.gearmind.application.repair.SaveRepairRequest;
import com.gearmind.application.repair.SaveRepairUseCase;
import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentRepository;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.repair.RepairStatus;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.domain.vehicle.VehicleRepository;
import com.gearmind.infrastructure.appointment.MySqlAppointmentRepository;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import com.gearmind.infrastructure.vehicle.MySqlVehicleRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReparacionFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private ComboBox<CustomerOption> cbCliente;
    @FXML
    private ComboBox<VehicleOption> cbVehiculo;
    @FXML
    private ComboBox<AppointmentOption> cbCita;
    @FXML
    private TextField txtImporteEstimado;
    @FXML
    private TextField txtImporteFinal;
    @FXML
    private ComboBox<String> cbEstado;
    @FXML
    private TextArea txtDescripcion;

    private Long empresaId;
    private SaveRepairUseCase saveRepairUseCase;
    private Repair existingRepair;
    private boolean saved = false;
    private final ObservableList<CustomerOption> allCustomers = FXCollections.observableArrayList();
    private final ObservableList<VehicleOption> allVehicles = FXCollections.observableArrayList();
    private final ObservableList<AppointmentOption> allAppointments = FXCollections.observableArrayList();
    private FilteredList<VehicleOption> filteredVehicles;
    private FilteredList<AppointmentOption> filteredAppointments;
    private boolean settingClienteProgrammatically = false;
    private boolean settingVehiculoProgrammatically = false;
    private String vehiculoSearchText = "";
    private final DateTimeFormatter appointmentFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void init(Long empresaId, SaveRepairUseCase saveRepairUseCase, Repair existingRepair) {
        this.empresaId = empresaId;
        this.saveRepairUseCase = saveRepairUseCase;
        this.existingRepair = existingRepair;

        configureCombos();
        configureEstadoCombo();

        if (existingRepair != null) {
            lblTitulo.setText("Editar reparación");
            if (existingRepair.getCitaId() != null) {
                selectAppointmentById(existingRepair.getCitaId());
            }
            if (existingRepair.getDescripcion() != null) {
                txtDescripcion.setText(existingRepair.getDescripcion());
            }
            if (existingRepair.getImporteEstimado() != null) {
                txtImporteEstimado.setText(existingRepair.getImporteEstimado().toPlainString());
            }
            if (existingRepair.getImporteFinal() != null) {
                txtImporteFinal.setText(existingRepair.getImporteFinal().toPlainString());
            }
            if (existingRepair.getClienteId() != null) {
                selectCustomerById(existingRepair.getClienteId());
            }
            if (existingRepair.getVehiculoId() != null) {
                selectVehicleById(existingRepair.getVehiculoId());
            }
            if (existingRepair.getEstado() != null) {
                cbEstado.getSelectionModel().select(mapStatusToLabel(existingRepair.getEstado()));
            }
        } else {
            lblTitulo.setText("Nueva reparación");
            cbEstado.getSelectionModel().select(mapStatusToLabel(RepairStatus.ABIERTA));
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private void configureCombos() {
        if (empresaId == null) {
            return;
        }

        CustomerRepository customerRepository = new MySqlCustomerRepository();
        List<Customer> customers = customerRepository.findByEmpresaId(empresaId);

        allCustomers.clear();
        for (Customer c : customers) {
            allCustomers.add(new CustomerOption(c.getId(), customerLabel(c)));
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
                updateVehicleFilter(null);
                return;
            }
            settingClienteProgrammatically = true;
            try {
                cbCliente.getEditor().setText(newVal.getLabel());
            } finally {
                settingClienteProgrammatically = false;
            }
            updateVehicleFilter(newVal.getId());
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

        VehicleRepository vehicleRepository = new MySqlVehicleRepository();
        List<Vehicle> vehicles = vehicleRepository.findByEmpresaId(empresaId);
        allVehicles.clear();
        for (Vehicle v : vehicles) {
            String label = buildVehicleLabel(v) + " (ID " + v.getId() + ")";
            allVehicles.add(new VehicleOption(v.getId(), v.getClienteId(), label));
        }

        filteredVehicles = new FilteredList<>(allVehicles, opt -> true);
        cbVehiculo.setItems(filteredVehicles);
        cbVehiculo.setEditable(true);
        cbVehiculo.setConverter(new StringConverter<>() {
            @Override
            public String toString(VehicleOption object) {
                return object == null ? "" : object.getLabel();
            }

            @Override
            public VehicleOption fromString(String string) {
                if (string == null) {
                    return null;
                }
                String s = string.trim().toLowerCase(Locale.ROOT);
                if (s.isBlank()) {
                    return null;
                }
                return allVehicles.stream().filter(o -> o.getLabel().toLowerCase(Locale.ROOT).equals(s)).findFirst().orElse(null);
            }
        });

        cbVehiculo.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            if (settingVehiculoProgrammatically) {
                return;
            }

            VehicleOption selected = cbVehiculo.getSelectionModel().getSelectedItem();
            String nt = (newV == null ? "" : newV).trim();

            if (selected != null && selected.getLabel() != null
                    && selected.getLabel().equalsIgnoreCase(nt)) {
                return;
            }

            vehiculoSearchText = nt.toLowerCase(Locale.ROOT);
            updateVehicleFilter(cbCliente.getValue() != null ? cbCliente.getValue().getId() : null);

            if (cbVehiculo.isFocused() && !cbVehiculo.isShowing()) {
                cbVehiculo.show();
            }
        });

        cbVehiculo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                updateAppointmentFilter(null);
                return;
            }
            settingVehiculoProgrammatically = true;
            try {
                cbVehiculo.getEditor().setText(newVal.getLabel());
            } finally {
                settingVehiculoProgrammatically = false;
            }
            updateAppointmentFilter(newVal.getId());
        });

        cbVehiculo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(VehicleOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getLabel());
            }
        });
        cbVehiculo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(VehicleOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getLabel());
            }
        });
        AppointmentRepository appointmentRepository = new MySqlAppointmentRepository();
        List<Appointment> appointments = appointmentRepository.findByEmpresa(empresaId);
        allAppointments.clear();
        for (Appointment appointment : appointments) {
            String label = appointment.getDateTime() != null ? appointment.getDateTime().format(appointmentFormatter) : "Sin fecha";
            allAppointments.add(new AppointmentOption(appointment.getId(), appointment.getVehicleId(), label));
        }

        filteredAppointments = new FilteredList<>(allAppointments, opt -> true);
        cbCita.setItems(filteredAppointments);
        cbCita.setConverter(new StringConverter<>() {
            @Override
            public String toString(AppointmentOption object) {
                return object == null ? "" : object.getLabel();
            }

            @Override
            public AppointmentOption fromString(String string) {
                if (string == null) {
                    return null;
                }
                String s = string.trim().toLowerCase(Locale.ROOT);
                if (s.isBlank()) {
                    return null;
                }
                return allAppointments.stream().filter(o -> o.getLabel().toLowerCase(Locale.ROOT).equals(s)).findFirst().orElse(null);
            }
        });

        cbCita.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AppointmentOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getLabel());
            }
        });
        cbCita.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AppointmentOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getLabel());
            }
        });
    }

    private String customerLabel(Customer customer) {
        if (customer == null) {
            return "";
        }
        StringBuilder label = new StringBuilder();
        if (customer.getNombre() != null) {
            label.append(customer.getNombre());
        }
        if (customer.getDni() != null && !customer.getDni().isBlank()) {
            label.append(" · DNI ").append(customer.getDni());
        }
        label.append(" (ID ").append(customer.getId()).append(")");
        return label.toString();
    }

    private void updateVehicleFilter(Long clienteId) {
        if (filteredVehicles == null) {
            return;
        }
        filteredVehicles.setPredicate(opt -> {
            boolean matchCliente = clienteId == null || opt.getClienteId() == null || opt.getClienteId().equals(clienteId);
            boolean matchTexto = vehiculoSearchText == null || vehiculoSearchText.isBlank()
                    || opt.getLabel().toLowerCase(Locale.ROOT).contains(vehiculoSearchText);
            return matchCliente && matchTexto;
        });

        VehicleOption selected = cbVehiculo.getSelectionModel().getSelectedItem();
        if (selected != null && clienteId != null && selected.getClienteId() != null
                && !selected.getClienteId().equals(clienteId)) {
            cbVehiculo.getSelectionModel().clearSelection();
            cbVehiculo.getEditor().clear();
            updateAppointmentFilter(null);
        }
    }

    private void updateAppointmentFilter(Long vehicleId) {
        if (filteredAppointments == null) {
            return;
        }
        filteredAppointments.setPredicate(opt -> vehicleId == null || opt.getVehicleId() == null || opt.getVehicleId().equals(vehicleId));

        AppointmentOption selected = cbCita.getSelectionModel().getSelectedItem();
        boolean selectedMatches = selected != null && (vehicleId == null || selected.getVehicleId() == null || selected.getVehicleId().equals(vehicleId));

        if (filteredAppointments.size() == 1) {
            cbCita.getSelectionModel().select(filteredAppointments.get(0));
        } else if (!selectedMatches) {
            cbCita.getSelectionModel().clearSelection();
        }
    }

    private void configureEstadoCombo() {
        cbEstado.getItems().clear();
        for (RepairStatus status : RepairStatus.values()) {
            cbEstado.getItems().add(mapStatusToLabel(status));
        }
    }

    private void selectCustomerById(Long customerId) {
        if (customerId == null) {
            return;
        }
        for (CustomerOption opt : allCustomers) {
            if (opt.getId().equals(customerId)) {
                cbCliente.getSelectionModel().select(opt);
                cbCliente.getEditor().setText(opt.getLabel());
                updateVehicleFilter(customerId);
                return;
            }
        }
    }

    private void selectVehicleById(Long vehicleId) {
        if (vehicleId == null) {
            return;
        }
        for (VehicleOption opt : allVehicles) {
            if (opt.getId().equals(vehicleId)) {
                cbVehiculo.getSelectionModel().select(opt);
                cbVehiculo.getEditor().setText(opt.getLabel());
                updateAppointmentFilter(opt.getId());
                return;
            }
        }
    }

    private void selectAppointmentById(Long appointmentId) {
        if (appointmentId == null) {
            return;
        }
        for (AppointmentOption opt : allAppointments) {
            if (opt.getId().equals(appointmentId)) {
                cbCita.getSelectionModel().select(opt);
                return;
            }
        }
    }

    @FXML
    private void onCancelar() {
        close();
    }

    @FXML
    private void onGuardar() {
        try {
            SaveRepairRequest request = buildRequest();
            saveRepairUseCase.execute(request);
            saved = true;
            close();
        } catch (Exception e) {
            showError("Error al guardar la reparación: " + e.getMessage());
        }
    }

    private SaveRepairRequest buildRequest() {
        if (empresaId == null) {
            throw new IllegalArgumentException("No se ha establecido la empresa para la reparación.");
        }

        CustomerOption clienteOpt = cbCliente.getValue();
        if (clienteOpt == null) {
            throw new IllegalArgumentException("Debes seleccionar un cliente.");
        }

        VehicleOption vehiculoOpt = cbVehiculo.getValue();
        if (vehiculoOpt == null) {
            throw new IllegalArgumentException("Debes seleccionar un vehículo.");
        }

        AppointmentOption citaOpt = cbCita.getValue();
        Long citaId = citaOpt != null ? citaOpt.getId() : null;
        String descripcion = txtDescripcion.getText();
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria.");
        }

        SaveRepairRequest request = new SaveRepairRequest();
        if (existingRepair != null) {
            request.setId(existingRepair.getId());
        }

        request.setEmpresaId(empresaId);
        request.setCitaId(citaId);
        request.setClienteId(clienteOpt.getId());
        request.setVehiculoId(vehiculoOpt.getId());
        request.setDescripcion(descripcion);
        request.setImporteEstimado(parseDecimalOrNull(txtImporteEstimado.getText(), "El importe estimado no es válido."));
        request.setImporteFinal(parseDecimalOrNull(txtImporteFinal.getText(), "El importe final no es válido."));

        String estadoLabel = cbEstado.getValue();
        RepairStatus estado = mapLabelToStatus(estadoLabel);
        request.setEstado(estado != null ? estado : RepairStatus.ABIERTA);

        return request;
    }

    private BigDecimal parseDecimalOrNull(String text, String errorMessage) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = trimmed.replace(',', '.');
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String mapStatusToLabel(RepairStatus status) {
        if (status == null) {
            return "Abierta";
        }
        return switch (status) {
            case ABIERTA ->
                "Abierta";
            case EN_PROCESO ->
                "En proceso";
            case FINALIZADA ->
                "Finalizada";
            case FACTURADA ->
                "Facturada";
            case CANCELADA ->
                "Cancelada";
        };
    }

    private RepairStatus mapLabelToStatus(String label) {
        if (label == null) {
            return null;
        }
        return switch (label) {
            case "Abierta" ->
                RepairStatus.ABIERTA;
            case "En proceso" ->
                RepairStatus.EN_PROCESO;
            case "Finalizada" ->
                RepairStatus.FINALIZADA;
            case "Facturada" ->
                RepairStatus.FACTURADA;
            case "Cancelada" ->
                RepairStatus.CANCELADA;
            default ->
                null;
        };
    }

    private String buildVehicleLabel(Vehicle vehicle) {
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
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(vehicle.getModelo());
        }
        return sb.toString();
    }

    private void close() {
        Stage stage = (Stage) lblTitulo.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    private static class CustomerOption {

        private final Long id;
        private final String label;

        CustomerOption(Long id, String label) {
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

    private static class VehicleOption {

        private final Long id;
        private final Long clienteId;
        private final String label;

        VehicleOption(Long id, Long clienteId, String label) {
            this.id = id;
            this.clienteId = clienteId;
            this.label = label;
        }

        public Long getId() {
            return id;
        }

        public Long getClienteId() {
            return clienteId;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class AppointmentOption {

        private final Long id;
        private final Long vehicleId;
        private final String label;

        private AppointmentOption(Long id, Long vehicleId, String label) {
            this.id = id;
            this.vehicleId = vehicleId;
            this.label = label;
        }

        public Long getId() {
            return id;
        }

        public Long getVehicleId() {
            return vehicleId;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}