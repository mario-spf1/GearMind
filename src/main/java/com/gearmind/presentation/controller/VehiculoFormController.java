package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.vehicle.SaveVehicleRequest;
import com.gearmind.application.vehicle.SaveVehicleUseCase;
import com.gearmind.common.exception.DuplicateException;
import com.gearmind.common.exception.ValidationException;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.domain.vehicle.VehicleRepository;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import com.gearmind.infrastructure.vehicle.MySqlVehicleRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class VehiculoFormController {

    @FXML
    private ComboBox<Customer> cmbCliente;
    @FXML
    private TextField txtMatricula;
    @FXML
    private TextField txtMarca;
    @FXML
    private TextField txtModelo;
    @FXML
    private TextField txtYear;
    @FXML
    private TextField txtVin;

    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnCancelar;

    private final SaveVehicleUseCase saveVehicleUseCase;
    private final CustomerRepository customerRepository;

    private Vehicle currentVehicle;
    private Runnable onSaved;

    public VehiculoFormController() {
        VehicleRepository vehicleRepository = new MySqlVehicleRepository();
        this.customerRepository = new MySqlCustomerRepository();

        this.saveVehicleUseCase = new SaveVehicleUseCase(vehicleRepository, customerRepository);
    }

    @FXML
    private void initialize() {
        // cómo se muestran los clientes en el combo
        cmbCliente.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String telefono = item.getTelefono() != null ? item.getTelefono() : "";
                    setText(item.getNombre() + (telefono.isBlank() ? "" : " (" + telefono + ")"));
                }
            }
        });
        cmbCliente.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String telefono = item.getTelefono() != null ? item.getTelefono() : "";
                    setText(item.getNombre() + (telefono.isBlank() ? "" : " (" + telefono + ")"));
                }
            }
        });

        loadClientes();

        // Si ya han llamado a setVehicle antes de initialize
        if (currentVehicle != null) {
            fillForm(currentVehicle);
        }
    }

    private void loadClientes() {
        long empresaId = AuthContext.getEmpresaId();

        // AJUSTA este método al que tengas realmente en tu CustomerRepository
        // (por ejemplo: findByEmpresaId, findAllByEmpresa, etc.)
        List<Customer> customers = customerRepository.findByEmpresaId(empresaId);

        cmbCliente.getItems().setAll(customers);
    }

    public void setVehicle(Vehicle vehiculo) {
        this.currentVehicle = vehiculo;
        // Si la vista ya está inicializada rellenamos
        if (cmbCliente != null) {
            fillForm(vehiculo);
        }
    }

    private void fillForm(Vehicle v) {
        if (v == null) {
            return;
        }

        txtMatricula.setText(v.getMatricula());
        txtMarca.setText(v.getMarca());
        txtModelo.setText(v.getModelo());
        txtYear.setText(v.getYear() != null ? String.valueOf(v.getYear()) : "");
        txtVin.setText(v.getVin());

        // seleccionar cliente
        if (v.getClienteId() != null) {
            for (Customer c : cmbCliente.getItems()) {
                // usamos == para evitar el problema del "long cannot be dereferenced"
                if (c.getId() == v.getClienteId()) {
                    cmbCliente.getSelectionModel().select(c);
                    break;
                }
            }
        }
    }

    @FXML
    private void onGuardar() {
        try {
            Customer cliente = cmbCliente.getValue();
            if (cliente == null) {
                showWarning("Debe seleccionar un cliente.");
                return;
            }

            SaveVehicleRequest req = new SaveVehicleRequest();
            if (currentVehicle != null) {
                req.setId(currentVehicle.getId());
            }
            req.setClienteId(cliente.getId());
            req.setMatricula(txtMatricula.getText());
            req.setMarca(txtMarca.getText());
            req.setModelo(txtModelo.getText());

            String yearText = txtYear.getText();
            if (yearText != null && !yearText.isBlank()) {
                try {
                    req.setYear(Integer.parseInt(yearText));
                } catch (NumberFormatException e) {
                    showWarning("El año debe ser un número.");
                    return;
                }
            }
            req.setVin(txtVin.getText());

            Vehicle saved = saveVehicleUseCase.execute(req);
            this.currentVehicle = saved;

            if (onSaved != null) {
                onSaved.run();
            }

            close();

        } catch (ValidationException | DuplicateException ex) {
            showWarning(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Se ha producido un error al guardar el vehículo.");
        }
    }

    @FXML
    private void onCancelar() {
        close();
    }

    private void close() {
        Stage stage = (Stage) txtMatricula.getScene().getWindow();
        stage.close();
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
}
