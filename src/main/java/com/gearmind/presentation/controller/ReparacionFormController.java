package com.gearmind.presentation.controller;

import com.gearmind.application.repair.SaveRepairRequest;
import com.gearmind.application.repair.SaveRepairUseCase;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.repair.RepairStatus;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.domain.vehicle.VehicleRepository;
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
    private TextField txtCitaId;
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
    private FilteredList<VehicleOption> filteredVehicles;
    private boolean settingClienteProgrammatically = false;
    private boolean settingVehiculoProgrammatically = false;
    private String vehiculoSearchText = "";

    public void init(Long empresaId, SaveRepairUseCase saveRepairUseCase, Repair existingRepair) {
        this.empresaId = empresaId;
        this.saveRepairUseCase = saveRepairUseCase;
        this.existingRepair = existingRepair;

        configureCombos();
        configureEstadoCombo();

        if (existingRepair != null) {
            lblTitulo.setText("Editar reparación");
            if (existingRepair.getCitaId() != null) {
                txtCitaId.setText(String.valueOf(existingRepair.getCitaId()));
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
                return;
            }
            settingVehiculoProgrammatically = true;
            try {
                cbVehiculo.getEditor().setText(newVal.getLabel());
            } finally {
                settingVehiculoProgrammatically = false;
            }
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

        Long citaId = null;
        String citaStr = txtCitaId.getText();
        if (citaStr != null && !citaStr.isBlank()) {
            try {
                citaId = Long.parseLong(citaStr.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El ID de la cita debe ser numérico.");
            }
        }

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
}
