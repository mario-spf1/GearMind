package com.gearmind.presentation.controller;

import com.gearmind.application.budget.GetBudgetUseCase;
import com.gearmind.application.budget.SaveBudgetRequest;
import com.gearmind.application.budget.SaveBudgetUseCase;
import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.domain.budget.Budget;
import com.gearmind.domain.budget.BudgetLine;
import com.gearmind.domain.budget.BudgetStatus;
import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.infrastructure.budget.BudgetPdfGenerator;
import com.gearmind.infrastructure.budget.BudgetPdfStorage;
import com.gearmind.infrastructure.budget.MySqlBudgetRepository;
import com.gearmind.infrastructure.company.MySqlEmpresaRepository;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import com.gearmind.infrastructure.repair.MySqlRepairRepository;
import com.gearmind.infrastructure.vehicle.MySqlVehicleRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PresupuestoFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblEmpresa;
    @FXML
    private ComboBox<EmpresaOption> cmbEmpresa;
    @FXML
    private HBox boxEmpresa;

    @FXML
    private ComboBox<ClienteOption> cmbCliente;
    @FXML
    private ComboBox<VehiculoOption> cmbVehiculo;
    @FXML
    private ComboBox<ReparacionOption> cmbReparacion;
    @FXML
    private ComboBox<String> cmbEstado;
    @FXML
    private TextArea txtObservaciones;

    @FXML
    private TableView<BudgetLine> tblLineas;
    @FXML
    private TableColumn<BudgetLine, String> colDescripcion;
    @FXML
    private TableColumn<BudgetLine, BigDecimal> colCantidad;
    @FXML
    private TableColumn<BudgetLine, BigDecimal> colPrecio;
    @FXML
    private TableColumn<BudgetLine, BigDecimal> colTotal;
    @FXML
    private Label lblTotal;
    @FXML
    private Button btnGuardar;

    private final ObservableList<EmpresaOption> empresas = FXCollections.observableArrayList();
    private final ObservableList<ClienteOption> clientes = FXCollections.observableArrayList();
    private final ObservableList<VehiculoOption> vehiculos = FXCollections.observableArrayList();
    private final ObservableList<ReparacionOption> reparaciones = FXCollections.observableArrayList();
    private final ObservableList<BudgetLine> lineas = FXCollections.observableArrayList();

    private Long editingId;
    private boolean saved = false;

    private final MySqlBudgetRepository budgetRepository;
    private final SaveBudgetUseCase saveBudgetUseCase;
    private final GetBudgetUseCase getBudgetUseCase;
    private final MySqlEmpresaRepository empresaRepository;
    private final MySqlCustomerRepository customerRepository;
    private final MySqlVehicleRepository vehicleRepository;
    private final MySqlRepairRepository repairRepository;
    private final BudgetPdfGenerator pdfGenerator;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.getDefault()));

    public PresupuestoFormController() {
        this.budgetRepository = new MySqlBudgetRepository();
        this.saveBudgetUseCase = new SaveBudgetUseCase(budgetRepository);
        this.getBudgetUseCase = new GetBudgetUseCase(budgetRepository);
        this.empresaRepository = new MySqlEmpresaRepository();
        this.customerRepository = new MySqlCustomerRepository();
        this.vehicleRepository = new MySqlVehicleRepository();
        this.repairRepository = new MySqlRepairRepository();
        this.pdfGenerator = new BudgetPdfGenerator();
    }

    public boolean isSaved() {
        return saved;
    }

    public void initForNew() {
        editingId = null;
        if (lblTitulo != null) {
            lblTitulo.setText("Nuevo presupuesto");
        }
        prepareFormForEmpresa();
        if (lineas.isEmpty()) {
            lineas.add(newLine());
        }
    }

    public void initForEdit(long presupuestoId) {
        editingId = presupuestoId;
        if (lblTitulo != null) {
            lblTitulo.setText("Editar presupuesto");
        }

        Optional<Budget> budgetOpt = getBudgetUseCase.execute(presupuestoId);
        if (budgetOpt.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No se encontró el presupuesto.").showAndWait();
            return;
        }

        Budget budget = budgetOpt.get();
        prepareFormForEmpresa();

        if (AuthContext.isSuperAdmin()) {
            EmpresaOption empresa = empresas.stream().filter(e -> e.id == budget.getEmpresaId()).findFirst().orElse(null);
            if (empresa != null) {
                cmbEmpresa.getSelectionModel().select(empresa);
            }
            loadDependentData(getEmpresaId());
        }

        ClienteOption cliente = clientes.stream().filter(c -> c.id == budget.getClienteId()).findFirst().orElse(null);
        if (cliente != null) {
            cmbCliente.getSelectionModel().select(cliente);
        }

        VehiculoOption vehiculo = vehiculos.stream().filter(v -> v.id == budget.getVehiculoId()).findFirst().orElse(null);
        if (vehiculo != null) {
            cmbVehiculo.getSelectionModel().select(vehiculo);
        }

        if (budget.getReparacionId() != null) {
            ReparacionOption reparacion = reparaciones.stream().filter(r -> r.id == budget.getReparacionId()).findFirst().orElse(null);
            if (reparacion != null) {
                cmbReparacion.getSelectionModel().select(reparacion);
            }
        }

        if (cmbEstado != null) {
            cmbEstado.getSelectionModel().select(mapStatusToLabel(budget.getEstado()));
        }
        if (txtObservaciones != null) {
            txtObservaciones.setText(budget.getObservaciones());
        }

        List<BudgetLine> existingLines = budgetRepository.findLinesByBudgetId(presupuestoId);
        lineas.setAll(existingLines);
        if (lineas.isEmpty()) {
            lineas.add(newLine());
        }
        updateTotals();
    }

    @FXML
    private void initialize() {
        if (btnGuardar != null) {
            btnGuardar.setDisable(true);
        }

        setupStatusCombo();
        setupTable();

        if (cmbEmpresa != null) {
            cmbEmpresa.setItems(empresas);
        }
        if (cmbCliente != null) {
            cmbCliente.setItems(clientes);
        }
        if (cmbVehiculo != null) {
            cmbVehiculo.setItems(vehiculos);
        }
        if (cmbReparacion != null) {
            cmbReparacion.setItems(reparaciones);
        }

        if (cmbEmpresa != null) {
            cmbEmpresa.setOnAction(e -> loadDependentData(getEmpresaId()));
        }
        if (cmbVehiculo != null) {
            cmbVehiculo.setOnAction(e -> syncClienteFromVehiculo());
        }
        if (cmbReparacion != null) {
            cmbReparacion.setOnAction(e -> syncFromReparacion());
        }

        if (cmbCliente != null) {
            cmbCliente.setOnAction(e -> enableSaveIfReady());
        }
        if (cmbVehiculo != null) {
            cmbVehiculo.setOnAction(e -> enableSaveIfReady());
        }
        if (txtObservaciones != null) {
            txtObservaciones.textProperty().addListener((obs, oldV, newV) -> enableSaveIfReady());
        }
    }

    @FXML
    private void onAgregarLinea() {
        lineas.add(newLine());
        updateTotals();
    }

    @FXML
    private void onEliminarLinea() {
        BudgetLine selected = tblLineas.getSelectionModel().getSelectedItem();
        if (selected != null) {
            lineas.remove(selected);
            updateTotals();
        }
    }

    @FXML
    private void onGuardar() {
        try {
            long empresaId = getEmpresaId();
            if (empresaId == 0L) {
                new Alert(Alert.AlertType.WARNING, "Selecciona una empresa válida.").showAndWait();
                return;
            }
            ClienteOption cliente = cmbCliente != null ? cmbCliente.getValue() : null;
            VehiculoOption vehiculo = cmbVehiculo != null ? cmbVehiculo.getValue() : null;

            if (cliente == null) {
                new Alert(Alert.AlertType.WARNING, "Selecciona un cliente.").showAndWait();
                return;
            }
            if (vehiculo == null) {
                new Alert(Alert.AlertType.WARNING, "Selecciona un vehículo.").showAndWait();
                return;
            }

            List<BudgetLine> lines = lineas.stream()
                    .filter(l -> l.getDescripcion() != null && !l.getDescripcion().isBlank())
                    .toList();

            if (lines.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Añade al menos una línea con descripción.").showAndWait();
                return;
            }

            SaveBudgetRequest request = new SaveBudgetRequest();
            request.setId(editingId);
            request.setEmpresaId(empresaId);
            request.setClienteId(cliente.id);
            request.setVehiculoId(vehiculo.id);
            ReparacionOption reparacion = cmbReparacion != null ? cmbReparacion.getValue() : null;
            if (reparacion != null && reparacion.id != null) {
                request.setReparacionId(reparacion.id);
            }
            request.setEstado(mapLabelToStatus(cmbEstado != null ? cmbEstado.getValue() : null));
            request.setObservaciones(txtObservaciones != null ? txtObservaciones.getText() : null);
            request.setLineas(lines);

            Budget savedBudget = saveBudgetUseCase.execute(request);
            generatePdf(savedBudget, lines);

            saved = true;
            new Alert(Alert.AlertType.INFORMATION, "Presupuesto guardado y PDF generado en:\n" + BudgetPdfStorage.resolvePath(savedBudget.getId())).showAndWait();

            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            stage.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar el presupuesto: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onCancelar() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }

    private void prepareFormForEmpresa() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();
        if (!isSuperAdmin) {
            if (lblEmpresa != null) {
                lblEmpresa.setVisible(false);
                lblEmpresa.setManaged(false);
            }
            if (boxEmpresa != null) {
                boxEmpresa.setVisible(false);
                boxEmpresa.setManaged(false);
            }
            loadDependentData(getEmpresaId());
            enableSaveIfReady();
            return;
        }

        if (lblEmpresa != null) {
            lblEmpresa.setVisible(true);
            lblEmpresa.setManaged(true);
        }
        if (boxEmpresa != null) {
            boxEmpresa.setVisible(true);
            boxEmpresa.setManaged(true);
        }

        empresas.setAll(empresaRepository.findAll().stream()
                .sorted(Comparator.comparing(Empresa::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(e -> new EmpresaOption(e.getId(), e.getNombre()))
                .toList());
        if (!empresas.isEmpty() && cmbEmpresa != null && cmbEmpresa.getValue() == null) {
            cmbEmpresa.getSelectionModel().selectFirst();
        }
    }

    private void loadDependentData(long empresaId) {
        if (empresaId == 0L) {
            return;
        }

        List<Customer> customers = customerRepository.findByEmpresaId(empresaId);
        clientes.setAll(customers.stream()
                .sorted(Comparator.comparing(Customer::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(c -> new ClienteOption(c.getId(), c.getNombre()))
                .toList());

        List<Vehicle> vehicles = vehicleRepository.findByEmpresaId(empresaId);
        vehiculos.setAll(vehicles.stream()
                .sorted(Comparator.comparing(Vehicle::getMatricula, String.CASE_INSENSITIVE_ORDER))
                .map(v -> new VehiculoOption(v.getId(), vehicleLabel(v), v.getClienteId()))
                .toList());

        List<Repair> repairs = repairRepository.findByEmpresa(empresaId);
        List<ReparacionOption> reparacionOptions = new ArrayList<>();
        reparacionOptions.add(new ReparacionOption(null, "Sin reparación", null, null));
        reparacionOptions.addAll(repairs.stream()
                .sorted(Comparator.comparing(Repair::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(r -> new ReparacionOption(r.getId(), repairLabel(r), r.getClienteId(), r.getVehiculoId()))
                .toList());
        reparaciones.setAll(reparacionOptions);

        if (cmbCliente != null && !clientes.isEmpty() && cmbCliente.getValue() == null) {
            cmbCliente.getSelectionModel().selectFirst();
        }
        if (cmbVehiculo != null && !vehiculos.isEmpty() && cmbVehiculo.getValue() == null) {
            cmbVehiculo.getSelectionModel().selectFirst();
        }

        if (cmbReparacion != null) {
            cmbReparacion.getSelectionModel().selectFirst();
        }

        enableSaveIfReady();
    }

    private void setupStatusCombo() {
        if (cmbEstado != null) {
            cmbEstado.setItems(FXCollections.observableArrayList("Borrador", "Enviado", "Aceptado", "Rechazado"));
            cmbEstado.getSelectionModel().select("Borrador");
        }
    }

    private void setupTable() {
        tblLineas.setItems(lineas);
        tblLineas.setEditable(true);
        tblLineas.setFixedCellSize(28);

        colDescripcion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescripcion()));
        colDescripcion.setCellFactory(TextFieldTableCell.forTableColumn());
        colDescripcion.setOnEditCommit(event -> {
            event.getRowValue().setDescripcion(event.getNewValue());
            updateTotals();
        });

        StringConverter<BigDecimal> decimalConverter = new StringConverter<>() {
            @Override
            public String toString(BigDecimal value) {
                if (value == null) {
                    return "0";
                }
                return value.stripTrailingZeros().toPlainString();
            }

            @Override
            public BigDecimal fromString(String string) {
                if (string == null || string.isBlank()) {
                    return BigDecimal.ZERO;
                }
                try {
                    return new BigDecimal(string.replace(",", "."));
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            }
        };

        colCantidad.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getCantidad()));
        colCantidad.setCellFactory(TextFieldTableCell.forTableColumn(decimalConverter));
        colCantidad.setOnEditCommit(event -> {
            event.getRowValue().setCantidad(event.getNewValue());
            updateTotals();
        });

        colPrecio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getPrecio()));
        colPrecio.setCellFactory(TextFieldTableCell.forTableColumn(decimalConverter));
        colPrecio.setOnEditCommit(event -> {
            event.getRowValue().setPrecio(event.getNewValue());
            updateTotals();
        });

        colTotal.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getTotal()));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(moneyFormat.format(item));
                }
            }
        });

        tblLineas.getItems().addListener((javafx.collections.ListChangeListener<BudgetLine>) c -> updateTotals());
    }

    private void updateTotals() {
        BigDecimal total = BigDecimal.ZERO;
        for (BudgetLine line : lineas) {
            BigDecimal cantidad = line.getCantidad() == null ? BigDecimal.ZERO : line.getCantidad();
            BigDecimal precio = line.getPrecio() == null ? BigDecimal.ZERO : line.getPrecio();
            BigDecimal lineTotal = cantidad.multiply(precio);
            line.setTotal(lineTotal);
            total = total.add(lineTotal);
        }

        if (lblTotal != null) {
            lblTotal.setText(moneyFormat.format(total));
        }

        tblLineas.refresh();
        enableSaveIfReady();
    }

    private BudgetLine newLine() {
        BudgetLine line = new BudgetLine();
        line.setDescripcion("");
        line.setCantidad(BigDecimal.ONE);
        line.setPrecio(BigDecimal.ZERO);
        line.setTotal(BigDecimal.ZERO);
        return line;
    }

    private long getEmpresaId() {
        if (AuthContext.isSuperAdmin()) {
            EmpresaOption selected = cmbEmpresa != null ? cmbEmpresa.getValue() : null;
            return selected != null ? selected.id : 0L;
        }
        return SessionManager.getInstance().getCurrentEmpresaId();
    }

    private void enableSaveIfReady() {
        if (btnGuardar == null) {
            return;
        }
        boolean hasCliente = cmbCliente != null && cmbCliente.getValue() != null;
        boolean hasVehiculo = cmbVehiculo != null && cmbVehiculo.getValue() != null;
        boolean hasLine = lineas.stream().anyMatch(l -> l.getDescripcion() != null && !l.getDescripcion().isBlank());
        btnGuardar.setDisable(!(hasCliente && hasVehiculo && hasLine));
    }

    private void syncClienteFromVehiculo() {
        VehiculoOption vehiculo = cmbVehiculo != null ? cmbVehiculo.getValue() : null;
        if (vehiculo == null || vehiculo.clienteId == null) {
            return;
        }
        ClienteOption cliente = clientes.stream().filter(c -> c.id == vehiculo.clienteId).findFirst().orElse(null);
        if (cliente != null) {
            cmbCliente.getSelectionModel().select(cliente);
        }
    }

    private void syncFromReparacion() {
        ReparacionOption reparacion = cmbReparacion != null ? cmbReparacion.getValue() : null;
        if (reparacion == null || reparacion.id == null) {
            return;
        }
        if (reparacion.vehiculoId != null) {
            VehiculoOption vehiculo = vehiculos.stream().filter(v -> v.id == reparacion.vehiculoId).findFirst().orElse(null);
            if (vehiculo != null) {
                cmbVehiculo.getSelectionModel().select(vehiculo);
            }
        }
        if (reparacion.clienteId != null) {
            ClienteOption cliente = clientes.stream().filter(c -> c.id == reparacion.clienteId).findFirst().orElse(null);
            if (cliente != null) {
                cmbCliente.getSelectionModel().select(cliente);
            }
        }
    }

    private void generatePdf(Budget budget, List<BudgetLine> lines) {
        Empresa empresa = empresaRepository.findById(budget.getEmpresaId()).orElse(null);
        Customer customer = customerRepository.findById(budget.getClienteId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(budget.getVehiculoId()).orElse(null);
        Path output = pdfGenerator.generate(budget, lines, empresa, customer, vehicle);
        if (output == null) {
            throw new RuntimeException("No se pudo generar el PDF.");
        }
    }

    private String mapStatusToLabel(BudgetStatus status) {
        if (status == null) {
            return "Borrador";
        }
        return switch (status) {
            case BORRADOR ->
                "Borrador";
            case ENVIADO ->
                "Enviado";
            case ACEPTADO ->
                "Aceptado";
            case RECHAZADO ->
                "Rechazado";
        };
    }

    private BudgetStatus mapLabelToStatus(String label) {
        if (label == null) {
            return BudgetStatus.BORRADOR;
        }
        return switch (label) {
            case "Enviado" ->
                BudgetStatus.ENVIADO;
            case "Aceptado" ->
                BudgetStatus.ACEPTADO;
            case "Rechazado" ->
                BudgetStatus.RECHAZADO;
            default ->
                BudgetStatus.BORRADOR;
        };
    }

    private String vehicleLabel(Vehicle vehicle) {
        if (vehicle == null) {
            return "";
        }
        String label = String.format("%s %s", safe(vehicle.getMarca()), safe(vehicle.getModelo())).trim();
        if (vehicle.getMatricula() != null && !vehicle.getMatricula().isBlank()) {
            return label + " - " + vehicle.getMatricula();
        }
        return label;
    }

    private String repairLabel(Repair repair) {
        if (repair == null) {
            return "";
        }
        String vehiculo = repair.getVehiculoEtiqueta() != null ? repair.getVehiculoEtiqueta() : "";
        String cliente = repair.getClienteNombre() != null ? repair.getClienteNombre() : "";
        String desc = repair.getDescripcion() != null ? repair.getDescripcion() : "";
        return String.format("#%d | %s | %s | %s", repair.getId(), vehiculo, cliente, desc);
    }

    private String safe(String value) {
        return value == null ? "" : value;
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

    private static class ClienteOption {

        private final long id;
        private final String nombre;

        private ClienteOption(long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private static class VehiculoOption {

        private final long id;
        private final String label;
        private final Long clienteId;

        private VehiculoOption(long id, String label, Long clienteId) {
            this.id = id;
            this.label = label;
            this.clienteId = clienteId;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class ReparacionOption {

        private final Long id;
        private final String label;
        private final Long clienteId;
        private final Long vehiculoId;

        private ReparacionOption(Long id, String label, Long clienteId, Long vehiculoId) {
            this.id = id;
            this.label = label;
            this.clienteId = clienteId;
            this.vehiculoId = vehiculoId;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
