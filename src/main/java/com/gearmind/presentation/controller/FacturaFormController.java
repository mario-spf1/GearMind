package com.gearmind.presentation.controller;

import com.gearmind.application.budget.GetBudgetUseCase;
import com.gearmind.application.budget.ListBudgetsUseCase;
import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.invoice.GetInvoiceUseCase;
import com.gearmind.application.invoice.SaveInvoiceRequest;
import com.gearmind.application.invoice.SaveInvoiceUseCase;
import com.gearmind.domain.budget.Budget;
import com.gearmind.domain.budget.BudgetLine;
import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceLine;
import com.gearmind.domain.invoice.InvoiceStatus;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.infrastructure.budget.MySqlBudgetRepository;
import com.gearmind.infrastructure.company.MySqlEmpresaRepository;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import com.gearmind.infrastructure.invoice.InvoicePdfGenerator;
import com.gearmind.infrastructure.invoice.InvoicePdfStorage;
import com.gearmind.infrastructure.invoice.MySqlInvoiceRepository;
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
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class FacturaFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblEmpresa;
    @FXML
    private ComboBox<EmpresaOption> cmbEmpresa;
    @FXML
    private HBox boxEmpresa;

    @FXML
    private ComboBox<PresupuestoOption> cmbPresupuesto;
    @FXML
    private ComboBox<ClienteOption> cmbCliente;
    @FXML
    private ComboBox<VehiculoOption> cmbVehiculo;
    @FXML
    private ComboBox<String> cmbEstado;
    @FXML
    private TextField txtIva;
    @FXML
    private TextArea txtObservaciones;

    @FXML
    private TableView<InvoiceLine> tblLineas;
    @FXML
    private TableColumn<InvoiceLine, String> colDescripcion;
    @FXML
    private TableColumn<InvoiceLine, BigDecimal> colCantidad;
    @FXML
    private TableColumn<InvoiceLine, BigDecimal> colPrecio;
    @FXML
    private TableColumn<InvoiceLine, BigDecimal> colTotal;
    @FXML
    private Label lblSubtotal;
    @FXML
    private Label lblIva;
    @FXML
    private Label lblTotal;
    @FXML
    private Button btnGuardar;

    private final ObservableList<EmpresaOption> empresas = FXCollections.observableArrayList();
    private final ObservableList<PresupuestoOption> presupuestos = FXCollections.observableArrayList();
    private final ObservableList<ClienteOption> clientes = FXCollections.observableArrayList();
    private final ObservableList<VehiculoOption> vehiculos = FXCollections.observableArrayList();
    private final ObservableList<InvoiceLine> lineas = FXCollections.observableArrayList();
    private final List<VehiculoOption> vehiculosEmpresa = new java.util.ArrayList<>();

    private Long editingId;
    private boolean saved = false;

    private final MySqlInvoiceRepository invoiceRepository;
    private final SaveInvoiceUseCase saveInvoiceUseCase;
    private final GetInvoiceUseCase getInvoiceUseCase;
    private final MySqlBudgetRepository budgetRepository;
    private final ListBudgetsUseCase listBudgetsUseCase;
    private final GetBudgetUseCase getBudgetUseCase;
    private final MySqlEmpresaRepository empresaRepository;
    private final MySqlCustomerRepository customerRepository;
    private final MySqlVehicleRepository vehicleRepository;
    private final InvoicePdfGenerator pdfGenerator;

    private final java.text.DecimalFormat moneyFormat = new java.text.DecimalFormat("#,##0.00", new java.text.DecimalFormatSymbols(Locale.getDefault()));

    public FacturaFormController() {
        this.invoiceRepository = new MySqlInvoiceRepository();
        this.saveInvoiceUseCase = new SaveInvoiceUseCase(invoiceRepository);
        this.getInvoiceUseCase = new GetInvoiceUseCase(invoiceRepository);
        this.budgetRepository = new MySqlBudgetRepository();
        this.listBudgetsUseCase = new ListBudgetsUseCase(budgetRepository);
        this.getBudgetUseCase = new GetBudgetUseCase(budgetRepository);
        this.empresaRepository = new MySqlEmpresaRepository();
        this.customerRepository = new MySqlCustomerRepository();
        this.vehicleRepository = new MySqlVehicleRepository();
        this.pdfGenerator = new InvoicePdfGenerator();
    }

    public boolean isSaved() {
        return saved;
    }

    public void initForNew() {
        editingId = null;
        if (lblTitulo != null) {
            lblTitulo.setText("Nueva factura");
        }
        prepareFormForEmpresa();
        if (AuthContext.isSuperAdmin()) {
            loadDependentData(getEmpresaId());
        }
        if (lineas.isEmpty()) {
            lineas.add(newLine());
        }
    }

    public void initForEdit(long facturaId) {
        editingId = facturaId;
        if (lblTitulo != null) {
            lblTitulo.setText("Editar factura");
        }

        Optional<Invoice> invoiceOpt = getInvoiceUseCase.execute(facturaId);
        if (invoiceOpt.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No se encontró la factura.").showAndWait();
            return;
        }

        Invoice invoice = invoiceOpt.get();
        prepareFormForEmpresa();

        if (AuthContext.isSuperAdmin()) {
            EmpresaOption empresa = empresas.stream().filter(e -> e.id == invoice.getEmpresaId()).findFirst().orElse(null);
            if (empresa != null) {
                cmbEmpresa.getSelectionModel().select(empresa);
            }
            loadDependentData(getEmpresaId());
        }

        PresupuestoOption presupuestoOption = presupuestos.stream()
                .filter(p -> p.id != null && p.id.equals(invoice.getPresupuestoId()))
                .findFirst()
                .orElse(null);
        if (presupuestoOption != null) {
            cmbPresupuesto.getSelectionModel().select(presupuestoOption);
        }

        ClienteOption cliente = clientes.stream().filter(c -> c.id == invoice.getClienteId()).findFirst().orElse(null);
        if (cliente != null) {
            cmbCliente.getSelectionModel().select(cliente);
            syncVehiculosFromCliente();
        }

        VehiculoOption vehiculo = vehiculos.stream().filter(v -> v.id == invoice.getVehiculoId()).findFirst().orElse(null);
        if (vehiculo != null) {
            cmbVehiculo.getSelectionModel().select(vehiculo);
        }

        if (cmbEstado != null) {
            cmbEstado.getSelectionModel().select(mapStatusToLabel(invoice.getEstado()));
        }
        if (txtObservaciones != null) {
            txtObservaciones.setText(invoice.getObservaciones());
        }

        List<InvoiceLine> existingLines = invoiceRepository.findLinesByInvoiceId(facturaId);
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
        if (cmbPresupuesto != null) {
            cmbPresupuesto.setItems(presupuestos);
        }
        if (cmbCliente != null) {
            cmbCliente.setItems(clientes);
        }
        if (cmbVehiculo != null) {
            cmbVehiculo.setItems(vehiculos);
        }

        if (cmbEmpresa != null) {
            cmbEmpresa.setOnAction(e -> loadDependentData(getEmpresaId()));
        }
        if (cmbPresupuesto != null) {
            cmbPresupuesto.setOnAction(e -> syncFromPresupuesto());
        }
        if (cmbCliente != null) {
            cmbCliente.setOnAction(e -> syncVehiculosFromCliente());
        }
        if (txtIva != null) {
            txtIva.textProperty().addListener((obs, o, n) -> updateTotals());
        }
        if (txtObservaciones != null) {
            txtObservaciones.textProperty().addListener((obs, o, n) -> enableSaveIfReady());
        }
    }

    @FXML
    private void onAgregarLinea() {
        lineas.add(newLine());
        updateTotals();
    }

    @FXML
    private void onEliminarLinea() {
        InvoiceLine selected = tblLineas.getSelectionModel().getSelectedItem();
        if (selected != null) {
            lineas.remove(selected);
            updateTotals();
        }
    }

    @FXML
    private void onGuardar() {
        try {
            if (tblLineas != null) {
                tblLineas.edit(-1, null);
            }
            long empresaId = getEmpresaId();
            if (empresaId == 0L) {
                new Alert(Alert.AlertType.WARNING, "Selecciona una empresa válida.").showAndWait();
                return;
            }
            PresupuestoOption presupuesto = cmbPresupuesto != null ? cmbPresupuesto.getValue() : null;
            if (presupuesto == null || presupuesto.id == null) {
                new Alert(Alert.AlertType.WARNING, "Selecciona un presupuesto.").showAndWait();
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

            List<InvoiceLine> lines = lineas.stream()
                    .filter(l -> l.getDescripcion() != null && !l.getDescripcion().isBlank())
                    .toList();

            if (lines.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Añade al menos una línea con descripción.").showAndWait();
                return;
            }

            SaveInvoiceRequest request = new SaveInvoiceRequest();
            request.setId(editingId);
            request.setEmpresaId(empresaId);
            request.setClienteId(cliente.id);
            request.setVehiculoId(vehiculo.id);
            request.setPresupuestoId(presupuesto.id);
            request.setEstado(mapLabelToStatus(cmbEstado != null ? cmbEstado.getValue() : null));
            request.setObservaciones(txtObservaciones != null ? txtObservaciones.getText() : null);
            request.setLineas(lines);
            request.setIvaPercent(parseIvaPercent());

            Invoice savedInvoice = saveInvoiceUseCase.execute(request);
            generatePdf(savedInvoice, lines);

            saved = true;
            new Alert(Alert.AlertType.INFORMATION, "Factura guardada y PDF generado en:\n" + InvoicePdfStorage.resolvePath(savedInvoice.getId())).showAndWait();

            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            stage.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar la factura: " + ex.getMessage()).showAndWait();
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

        List<Budget> budgets;
        if (AuthContext.isSuperAdmin()) {
            budgets = listBudgetsUseCase.listAllWithEmpresa();
        } else {
            budgets = listBudgetsUseCase.listByEmpresa(empresaId);
        }
        presupuestos.setAll(budgets.stream()
                .filter(b -> b.getEmpresaId() == empresaId)
                .sorted(Comparator.comparing(Budget::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(b -> new PresupuestoOption(b.getId(), budgetLabel(b), b.getClienteId(), b.getVehiculoId()))
                .toList());

        List<Customer> customers = customerRepository.findByEmpresaId(empresaId);
        clientes.setAll(customers.stream().sorted(Comparator.comparing(Customer::getNombre, String.CASE_INSENSITIVE_ORDER)).map(c -> new ClienteOption(c.getId(), c.getNombre())).toList());

        List<Vehicle> vehicles = vehicleRepository.findByEmpresaId(empresaId);
        vehiculosEmpresa.clear();
        vehiculosEmpresa.addAll(vehicles.stream().sorted(Comparator.comparing(Vehicle::getMatricula, String.CASE_INSENSITIVE_ORDER)).map(v -> new VehiculoOption(v.getId(), vehicleLabel(v), v.getClienteId())).toList());

        if (cmbPresupuesto != null && !presupuestos.isEmpty() && cmbPresupuesto.getValue() == null) {
            cmbPresupuesto.getSelectionModel().selectFirst();
            syncFromPresupuesto();
        }

        syncVehiculosFromCliente();
        enableSaveIfReady();
    }

    private void setupStatusCombo() {
        if (cmbEstado != null) {
            cmbEstado.setItems(FXCollections.observableArrayList("Pendiente", "Pagada", "Anulada", "Borrador"));
            cmbEstado.getSelectionModel().select("Pendiente");
        }
        if (txtIva != null) {
            txtIva.setText("21");
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

        tblLineas.getItems().addListener((javafx.collections.ListChangeListener<InvoiceLine>) c -> updateTotals());
    }

    private void updateTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (InvoiceLine line : lineas) {
            BigDecimal cantidad = line.getCantidad() == null ? BigDecimal.ZERO : line.getCantidad();
            BigDecimal precio = line.getPrecio() == null ? BigDecimal.ZERO : line.getPrecio();
            BigDecimal lineTotal = cantidad.multiply(precio);
            line.setTotal(lineTotal);
            subtotal = subtotal.add(lineTotal);
        }

        int ivaPercent = parseIvaPercent();
        BigDecimal iva = subtotal.multiply(BigDecimal.valueOf(ivaPercent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva);

        if (lblSubtotal != null) {
            lblSubtotal.setText(moneyFormat.format(subtotal));
        }
        if (lblIva != null) {
            lblIva.setText(moneyFormat.format(iva));
        }
        if (lblTotal != null) {
            lblTotal.setText(moneyFormat.format(total));
        }

        tblLineas.refresh();
        enableSaveIfReady();
    }

    private int parseIvaPercent() {
        if (txtIva == null || txtIva.getText() == null || txtIva.getText().isBlank()) {
            return 21;
        }
        try {
            return Integer.parseInt(txtIva.getText().trim());
        } catch (NumberFormatException e) {
            return 21;
        }
    }

    private InvoiceLine newLine() {
        InvoiceLine line = new InvoiceLine();
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
        boolean hasPresupuesto = cmbPresupuesto != null && cmbPresupuesto.getValue() != null;
        boolean hasCliente = cmbCliente != null && cmbCliente.getValue() != null;
        boolean hasVehiculo = cmbVehiculo != null && cmbVehiculo.getValue() != null;
        boolean hasLine = lineas.stream().anyMatch(l -> l.getDescripcion() != null && !l.getDescripcion().isBlank());
        btnGuardar.setDisable(!(hasPresupuesto && hasCliente && hasVehiculo && hasLine));
    }

    private void syncVehiculosFromCliente() {
        ClienteOption cliente = cmbCliente != null ? cmbCliente.getValue() : null;
        Long clienteId = cliente != null ? cliente.id : null;
        List<VehiculoOption> filtered = vehiculosEmpresa.stream().filter(v -> clienteId != null && clienteId.equals(v.clienteId)).toList();
        vehiculos.setAll(filtered);
        if (cmbVehiculo != null) {
            if (!vehiculos.isEmpty()) {
                VehiculoOption selected = cmbVehiculo.getValue();
                boolean keep = selected != null && vehiculos.stream().anyMatch(v -> v.id == selected.id);
                if (!keep) {
                    cmbVehiculo.getSelectionModel().selectFirst();
                }
            } else {
                cmbVehiculo.getSelectionModel().clearSelection();
            }
        }
        enableSaveIfReady();
    }

    private void syncFromPresupuesto() {
        PresupuestoOption presupuesto = cmbPresupuesto != null ? cmbPresupuesto.getValue() : null;
        if (presupuesto == null || presupuesto.id == null) {
            return;
        }
        Optional<Budget> budgetOpt = getBudgetUseCase.execute(presupuesto.id);
        if (budgetOpt.isEmpty()) {
            return;
        }
        Budget budget = budgetOpt.get();

        ClienteOption cliente = clientes.stream().filter(c -> c.id == budget.getClienteId()).findFirst().orElse(null);
        if (cliente != null) {
            cmbCliente.getSelectionModel().select(cliente);
            syncVehiculosFromCliente();
        }
        VehiculoOption vehiculo = vehiculos.stream().filter(v -> v.id == budget.getVehiculoId()).findFirst().orElse(null);
        if (vehiculo != null) {
            cmbVehiculo.getSelectionModel().select(vehiculo);
        }

        List<BudgetLine> budgetLines = budgetRepository.findLinesByBudgetId(budget.getId());
        lineas.setAll(budgetLines.stream().map(this::mapLine).toList());
        if (lineas.isEmpty()) {
            lineas.add(newLine());
        }
        updateTotals();
    }

    private InvoiceLine mapLine(BudgetLine line) {
        InvoiceLine mapped = new InvoiceLine();
        mapped.setProductoId(line.getProductoId());
        mapped.setDescripcion(line.getDescripcion());
        mapped.setCantidad(line.getCantidad());
        mapped.setPrecio(line.getPrecio());
        mapped.setTotal(line.getTotal());
        return mapped;
    }

    private void generatePdf(Invoice invoice, List<InvoiceLine> lines) {
        Empresa empresa = empresaRepository.findById(invoice.getEmpresaId()).orElse(null);
        Customer customer = customerRepository.findById(invoice.getClienteId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(invoice.getVehiculoId()).orElse(null);
        pdfGenerator.generate(invoice, lines, empresa, customer, vehicle);
    }

    private String mapStatusToLabel(InvoiceStatus status) {
        if (status == null) {
            return "Pendiente";
        }
        return switch (status) {
            case BORRADOR ->
                "Borrador";
            case PENDIENTE ->
                "Pendiente";
            case PAGADA ->
                "Pagada";
            case ANULADA ->
                "Anulada";
        };
    }

    private InvoiceStatus mapLabelToStatus(String label) {
        if (label == null) {
            return InvoiceStatus.PENDIENTE;
        }
        return switch (label) {
            case "Pagada" ->
                InvoiceStatus.PAGADA;
            case "Anulada" ->
                InvoiceStatus.ANULADA;
            case "Borrador" ->
                InvoiceStatus.BORRADOR;
            default ->
                InvoiceStatus.PENDIENTE;
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

    private String budgetLabel(Budget budget) {
        if (budget == null) {
            return "";
        }
        String vehiculo = budget.getVehiculoEtiqueta() != null ? budget.getVehiculoEtiqueta() : "";
        String cliente = budget.getClienteNombre() != null ? budget.getClienteNombre() : "";
        return String.format("#%d | %s | %s", budget.getId(), vehiculo, cliente);
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

    private static class PresupuestoOption {

        private final Long id;
        private final String label;
        private final Long clienteId;
        private final Long vehiculoId;

        private PresupuestoOption(Long id, String label, Long clienteId, Long vehiculoId) {
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
}
