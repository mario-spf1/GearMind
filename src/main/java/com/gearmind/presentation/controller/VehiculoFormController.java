package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.vehicle.SaveVehicleRequest;
import com.gearmind.application.vehicle.SaveVehicleUseCase;
import com.gearmind.common.exception.DuplicateException;
import com.gearmind.common.exception.ValidationException;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.domain.vehicle.VehicleRepository;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import com.gearmind.infrastructure.database.DataSourceFactory;
import com.gearmind.infrastructure.vehicle.MySqlVehicleRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;

public class VehiculoFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblEmpresa;
    @FXML
    private HBox boxEmpresa;
    @FXML
    private ComboBox<EmpresaOption> cmbEmpresa;
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

    private final SaveVehicleUseCase saveVehicleUseCase;
    private final CustomerRepository customerRepository;

    private final DataSource dataSource = DataSourceFactory.getDataSource();
    private final ObservableList<EmpresaOption> empresas = FXCollections.observableArrayList();
    private FilteredList<EmpresaOption> empresasFiltradas;
    private Vehicle currentVehicle;
    private Runnable onSaved;

    public VehiculoFormController() {
        VehicleRepository vehicleRepository = new MySqlVehicleRepository();
        this.customerRepository = new MySqlCustomerRepository();
        this.saveVehicleUseCase = new SaveVehicleUseCase(vehicleRepository, customerRepository);
    }

    @FXML
    private void initialize() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        if (lblTitulo != null) {
            lblTitulo.setText("Vehículo");
        }

        cmbCliente.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String tel = item.getTelefono() != null ? item.getTelefono() : "";
                    setText(item.getNombre() + (tel.isBlank() ? "" : " (" + tel + ")"));
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
                    String tel = item.getTelefono() != null ? item.getTelefono() : "";
                    setText(item.getNombre() + (tel.isBlank() ? "" : " (" + tel + ")"));
                }
            }
        });

        if (!isSuperAdmin) {
            if (lblEmpresa != null) {
                lblEmpresa.setVisible(false);
                lblEmpresa.setManaged(false);
            }
            if (boxEmpresa != null) {
                boxEmpresa.setVisible(false);
                boxEmpresa.setManaged(false);
            }
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            loadClientesByEmpresa(empresaId);

        } else {
            if (lblEmpresa != null) {
                lblEmpresa.setVisible(true);
                lblEmpresa.setManaged(true);
            }
            if (boxEmpresa != null) {
                boxEmpresa.setVisible(true);
                boxEmpresa.setManaged(true);
            }

            loadEmpresas();
            enableComboSearch(cmbEmpresa);

            cmbCliente.getItems().clear();
            cmbCliente.setDisable(true);

            if (cmbEmpresa != null) {
                cmbEmpresa.valueProperty().addListener((obs, o, n) -> {
                    Object v = cmbEmpresa.getValue();
                    if (v instanceof EmpresaOption eo) {
                        loadClientesByEmpresa(eo.id);
                        cmbCliente.setDisable(false);
                        if (currentVehicle != null) {
                            selectClienteFromVehicle(currentVehicle);
                        }
                    } else {
                        cmbCliente.getItems().clear();
                        cmbCliente.setDisable(true);
                    }
                });
            }
        }
    }

    public void setVehicle(Vehicle vehiculo) {
        this.currentVehicle = vehiculo;

        if (vehiculo == null) {
            if (lblTitulo != null) {
                lblTitulo.setText("Nuevo vehículo");
            }
            return;
        }

        if (lblTitulo != null) {
            lblTitulo.setText("Editar vehículo");
        }

        txtMatricula.setText(vehiculo.getMatricula());
        txtMarca.setText(vehiculo.getMarca());
        txtModelo.setText(vehiculo.getModelo());
        txtYear.setText(vehiculo.getYear() != null ? String.valueOf(vehiculo.getYear()) : "");
        txtVin.setText(vehiculo.getVin());

        if (AuthContext.isSuperAdmin()) {
            if (vehiculo.getEmpresaId() != null) {
                preselectEmpresa(vehiculo.getEmpresaId());
                cmbCliente.setDisable(false);
                selectClienteFromVehicle(vehiculo);
            }
        } else {
            selectClienteFromVehicle(vehiculo);
        }
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    private void preselectEmpresa(long empresaId) {
        if (cmbEmpresa == null) {
            return;
        }

        if (empresas.isEmpty()) {
            loadEmpresas();
        }

        for (EmpresaOption opt : empresas) {
            if (opt != null && opt.id == empresaId) {
                cmbEmpresa.getSelectionModel().select(opt);
                if (cmbEmpresa.isEditable()) {
                    cmbEmpresa.getEditor().setText(opt.nombre);
                }
                cmbEmpresa.setValue(opt);

                loadClientesByEmpresa(opt.id);
                return;
            }
        }
    }

    private void loadClientesByEmpresa(long empresaId) {
        List<Customer> customers = customerRepository.findByEmpresaId(empresaId);
        cmbCliente.getItems().setAll(customers);
    }

    private void selectClienteFromVehicle(Vehicle v) {
        if (v == null || v.getClienteId() == null) {
            return;
        }

        for (Customer c : cmbCliente.getItems()) {
            if (c != null && c.getId() == v.getClienteId()) {
                cmbCliente.getSelectionModel().select(c);
                break;
            }
        }
    }

    @FXML
    private void onGuardar() {
        try {
            long empresaId;

            if (AuthContext.isSuperAdmin()) {
                if (cmbEmpresa == null) {
                    showWarning("Selecciona una empresa.");
                    return;
                }

                Object v = cmbEmpresa.getValue();
                if (!(v instanceof EmpresaOption eo)) {
                    showWarning("Selecciona una empresa válida.");
                    return;
                }
                empresaId = eo.id;
            } else {
                empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            }

            Customer cliente = cmbCliente.getValue();
            if (cliente == null) {
                showWarning("Debe seleccionar un cliente.");
                return;
            }

            SaveVehicleRequest req = new SaveVehicleRequest();
            if (currentVehicle != null) {
                req.setId(currentVehicle.getId());
            }
            req.setEmpresaId(empresaId);
            req.setClienteId(cliente.getId());
            req.setMatricula(txtMatricula.getText());
            req.setMarca(txtMarca.getText());
            req.setModelo(txtModelo.getText());

            String yearText = txtYear.getText();
            if (yearText != null && !yearText.isBlank()) {
                try {
                    req.setYear(Integer.parseInt(yearText.trim()));
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
            showError("Se ha producido un error al guardar el vehículo.\n\n" + ex.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        close();
    }

    private void close() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }

    private void showWarning(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void loadEmpresas() {
        empresas.clear();
        String sql = "SELECT id, nombre FROM empresa ORDER BY nombre ASC";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                empresas.add(new EmpresaOption(rs.getLong("id"), rs.getString("nombre")));
            }
        } catch (Exception ex) {
            throw new RuntimeException("No se pudieron cargar las empresas", ex);
        }
    }

    private void enableComboSearch(ComboBox<EmpresaOption> combo) {
        if (combo == null) {
            return;
        }

        combo.setEditable(true);
        combo.setVisibleRowCount(10);
        final boolean[] internalChange = {false};

        combo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(EmpresaOption opt) {
                return opt == null ? "" : opt.nombre;
            }

            @Override
            public EmpresaOption fromString(String s) {
                if (s == null) {
                    return null;
                }
                String t = s.trim();
                if (t.isEmpty()) {
                    return null;
                }

                return empresas.stream().filter(e -> e.nombre != null && e.nombre.equalsIgnoreCase(t)).findFirst().orElse(null);
            }
        });

        empresasFiltradas = new FilteredList<>(empresas, e -> true);
        combo.setItems(empresasFiltradas);

        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EmpresaOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.nombre);
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(EmpresaOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.nombre);
            }
        });

        combo.valueProperty().addListener((obs, oldV, newV) -> {
            if (internalChange[0]) {
                return;
            }

            if (newV != null) {
                internalChange[0] = true;
                combo.getEditor().setText(newV.nombre);
                combo.setValue(newV);
                internalChange[0] = false;
            }
        });

        combo.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (internalChange[0]) {
                return;
            }

            String f = (newText == null ? "" : newText).toLowerCase(Locale.ROOT).trim();
            empresasFiltradas.setPredicate(opt -> f.isEmpty() || (opt.nombre != null && opt.nombre.toLowerCase(Locale.ROOT).contains(f))
            );

            if (!combo.isShowing() && combo.isFocused()) {
                combo.show();
            }
        });

        combo.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                return;
            }

            EmpresaOption match = combo.getConverter().fromString(combo.getEditor().getText());
            internalChange[0] = true;
            if (match != null) {
                combo.getSelectionModel().select(match);
                combo.setValue(match);
                combo.getEditor().setText(match.nombre);
            } else {
                combo.getSelectionModel().clearSelection();
                combo.setValue(null);
                combo.getEditor().clear();
            }
            internalChange[0] = false;
        });

        combo.setOnHidden(e -> {
            EmpresaOption match = combo.getConverter().fromString(combo.getEditor().getText());
            if (match != null) {
                internalChange[0] = true;
                combo.getSelectionModel().select(match);
                combo.setValue(match);
                combo.getEditor().setText(match.nombre);
                internalChange[0] = false;
            }
        });
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
