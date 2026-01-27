package com.gearmind.presentation.controller;

import com.gearmind.application.inventory.AdjustInventoryUseCase;
import com.gearmind.application.product.ListProductsUseCase;
import com.gearmind.domain.inventory.InventoryMovement;
import com.gearmind.domain.inventory.InventoryMovementType;
import com.gearmind.domain.product.Product;
import com.gearmind.domain.repair.Repair;
import com.gearmind.infrastructure.inventory.MySqlInventoryMovementRepository;
import com.gearmind.infrastructure.product.MySqlProductRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class ReparacionProductosController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label lblTitulo;
    @FXML
    private ComboBox<ProductOption> cmbProducto;
    @FXML
    private TextField txtCantidad;
    @FXML
    private TextArea txtNotas;
    @FXML
    private TableView<InventoryMovement> tblMovimientos;
    @FXML
    private TableColumn<InventoryMovement, String> colProducto;
    @FXML
    private TableColumn<InventoryMovement, String> colCantidad;
    @FXML
    private TableColumn<InventoryMovement, String> colFecha;
    @FXML
    private TableColumn<InventoryMovement, String> colNotas;
    @FXML
    private Button btnAgregar;

    private final ObservableList<ProductOption> productos = FXCollections.observableArrayList();
    private final ObservableList<InventoryMovement> movimientos = FXCollections.observableArrayList();

    private final ListProductsUseCase listProductsUseCase;
    private final AdjustInventoryUseCase adjustInventoryUseCase;
    private final MySqlInventoryMovementRepository movementRepository;

    private Repair repair;

    public ReparacionProductosController() {
        MySqlProductRepository productRepository = new MySqlProductRepository();
        this.listProductsUseCase = new ListProductsUseCase(productRepository);
        this.movementRepository = new MySqlInventoryMovementRepository();
        this.adjustInventoryUseCase = new AdjustInventoryUseCase(productRepository, movementRepository);
    }

    public void init(Repair repair) {
        this.repair = repair;
        if (lblTitulo != null && repair != null) {
            lblTitulo.setText("Productos usados en reparación #" + repair.getId());
        }
        loadProductos();
        loadMovimientos();
    }

    @FXML
    private void initialize() {
        if (cmbProducto != null) {
            cmbProducto.setItems(productos);
        }
        if (tblMovimientos != null) {
            tblMovimientos.setItems(movimientos);
            tblMovimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        if (colProducto != null) {
            colProducto.setCellValueFactory(c -> new SimpleStringProperty(productLabel(c.getValue())));
        }
        if (colCantidad != null) {
            colCantidad.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCantidad())));
        }
        if (colFecha != null) {
            colFecha.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue())));
        }
        if (colNotas != null) {
            colNotas.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getNotas())));
        }

        if (txtCantidad != null) {
            txtCantidad.setText("1");
        }
        movimientos.addListener((javafx.collections.ListChangeListener<InventoryMovement>) change -> updateTableHeight());
        updateTableHeight();
    }

    @FXML
    private void onAgregarProducto() {
        if (repair == null) {
            return;
        }
        ProductOption product = cmbProducto != null ? cmbProducto.getValue() : null;
        if (product == null) {
            new Alert(Alert.AlertType.WARNING, "Selecciona un producto.").showAndWait();
            return;
        }
        int cantidad = parseCantidad();
        if (cantidad <= 0) {
            new Alert(Alert.AlertType.WARNING, "La cantidad debe ser mayor que cero.").showAndWait();
            return;
        }
        String notas = txtNotas != null ? txtNotas.getText() : null;
        String referencia = referencia();

        try {
            adjustInventoryUseCase.execute(repair.getEmpresaId(), product.id, InventoryMovementType.SALIDA, cantidad, referencia, notas);
            if (txtCantidad != null) {
                txtCantidad.setText("1");
            }
            if (txtNotas != null) {
                txtNotas.clear();
            }
            loadMovimientos();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "No se pudo registrar el consumo: " + ex.getMessage()).showAndWait();
        }
    }

    private void loadProductos() {
        if (repair == null) {
            return;
        }
        List<Product> productList = listProductsUseCase.listByEmpresa(repair.getEmpresaId()).stream()
                .filter(Product::isActivo)
                .sorted(Comparator.comparing(Product::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
        productos.setAll(productList.stream().map(p -> new ProductOption(p.getId(), labelFor(p))).toList());
        if (!productos.isEmpty() && cmbProducto != null) {
            cmbProducto.getSelectionModel().selectFirst();
        }
    }

    private void loadMovimientos() {
        if (repair == null) {
            return;
        }
        movimientos.setAll(movementRepository.findByEmpresaAndReferencia(repair.getEmpresaId(), referencia()));
        if (tblMovimientos != null) {
            tblMovimientos.refresh();
        }
        updateTableHeight();
    }

    private void updateTableHeight() {
        if (tblMovimientos == null) {
            return;
        }
        int maxVisibleRows = 8;
        int rows = Math.min(movimientos.size(), maxVisibleRows);
        double headerHeight = 28;
        double tableHeight = headerHeight + rows * tblMovimientos.getFixedCellSize() + 2;
        tblMovimientos.setPrefHeight(tableHeight);
        tblMovimientos.setMinHeight(Region.USE_PREF_SIZE);
        tblMovimientos.setMaxHeight(Region.USE_PREF_SIZE);
        Platform.runLater(() -> {
            if (tblMovimientos.getScene() != null && tblMovimientos.getScene().getWindow() instanceof Stage stage) {
                stage.sizeToScene();
            }
        });
    }

    private String referencia() {
        return "Reparación #" + (repair != null ? repair.getId() : "");
    }

    private int parseCantidad() {
        if (txtCantidad == null || txtCantidad.getText() == null || txtCantidad.getText().isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(txtCantidad.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatDate(InventoryMovement movement) {
        if (movement == null || movement.getCreatedAt() == null) {
            return "";
        }
        return movement.getCreatedAt().format(DATE_FORMAT);
    }

    private String productLabel(InventoryMovement movement) {
        if (movement == null) {
            return "";
        }
        String nombre = safe(movement.getProductoNombre());
        String referencia = safe(movement.getProductoReferencia());
        if (!referencia.isBlank()) {
            return nombre + " · Ref " + referencia;
        }
        return nombre;
    }

    private String labelFor(Product product) {
        if (product == null) {
            return "";
        }
        String nombre = safe(product.getNombre());
        String referencia = safe(product.getReferencia());
        if (!referencia.isBlank()) {
            return nombre + " · Ref " + referencia;
        }
        return nombre;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class ProductOption {

        private final long id;
        private final String label;

        private ProductOption(long id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
