package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.product.SaveProductRequest;
import com.gearmind.application.product.SaveProductUseCase;
import com.gearmind.domain.product.Product;
import com.gearmind.infrastructure.database.DataSourceFactory;
import com.gearmind.infrastructure.product.MySqlProductRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

public class ProductoFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblEmpresa;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtReferencia;
    @FXML
    private TextField txtCategoria;
    @FXML
    private TextField txtStock;
    @FXML
    private TextField txtStockMinimo;
    @FXML
    private TextField txtPrecioCompra;
    @FXML
    private TextField txtPrecioVenta;
    @FXML
    private TextArea txtDescripcion;
    @FXML
    private Button btnGuardar;

    @FXML
    private HBox boxEmpresa;
    @FXML
    private ComboBox<EmpresaOption> cmbEmpresa;

    private Long editingId = null;
    private boolean saved = false;

    private final SaveProductUseCase saveProductUseCase;
    private final ObservableList<EmpresaOption> empresas = FXCollections.observableArrayList();
    private FilteredList<EmpresaOption> empresasFiltradas;

    private final DataSource dataSource = DataSourceFactory.getDataSource();

    public ProductoFormController() {
        this.saveProductUseCase = new SaveProductUseCase(new MySqlProductRepository());
    }

    public boolean isSaved() {
        return saved;
    }

    public void initForNew() {
        editingId = null;
        if (lblTitulo != null) {
            lblTitulo.setText("Nuevo producto");
        }

        if (AuthContext.isSuperAdmin()) {
            if (cmbEmpresa != null) {
                cmbEmpresa.getSelectionModel().clearSelection();
                cmbEmpresa.getEditor().clear();
                cmbEmpresa.setValue(null);
            }
        }
    }

    public void initForEdit(Product product) {
        editingId = product.getId();
        if (lblTitulo != null) {
            lblTitulo.setText("Editar producto");
        }

        txtNombre.setText(product.getNombre());
        txtReferencia.setText(product.getReferencia());
        txtCategoria.setText(product.getCategoria());
        txtStock.setText(formatInt(product.getStock()));
        txtStockMinimo.setText(formatInt(product.getStockMinimo()));
        txtPrecioCompra.setText(formatDecimal(product.getPrecioCompra()));
        txtPrecioVenta.setText(formatDecimal(product.getPrecioVenta()));
        txtDescripcion.setText(product.getDescripcion());

        if (AuthContext.isSuperAdmin()) {
            if (cmbEmpresa != null) {
                long empresaId = product.getEmpresaId();
                EmpresaOption opt = empresas.stream().filter(e -> e.id == empresaId).findFirst().orElse(null);

                if (opt != null) {
                    cmbEmpresa.getSelectionModel().select(opt);
                    if (cmbEmpresa.isEditable()) {
                        cmbEmpresa.getEditor().setText(opt.nombre);
                    }
                    cmbEmpresa.setValue(opt);
                } else {
                    cmbEmpresa.getSelectionModel().clearSelection();
                    if (cmbEmpresa.isEditable()) {
                        cmbEmpresa.getEditor().clear();
                    }
                    cmbEmpresa.setValue(null);
                }
            }
        }
    }

    @FXML
    private void initialize() {

        if (btnGuardar != null) {
            btnGuardar.setDisable(true);
        }
        txtNombre.textProperty().addListener((obs, oldV, newV) -> {
            if (btnGuardar != null) {
                btnGuardar.setDisable(newV == null || newV.trim().isEmpty());
            }
        });

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

        loadEmpresas();
        enableComboSearch(cmbEmpresa);
    }

    @FXML
    private void onGuardar() {
        try {
            long empresaId;

            if (AuthContext.isSuperAdmin()) {
                if (cmbEmpresa == null) {
                    new Alert(Alert.AlertType.WARNING, "Selecciona una empresa.").showAndWait();
                    return;
                }

                Object v = cmbEmpresa.getValue();
                if (!(v instanceof EmpresaOption eo)) {
                    new Alert(Alert.AlertType.WARNING, "Selecciona una empresa válida.").showAndWait();
                    return;
                }

                empresaId = eo.id;
            } else {
                empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            }

            String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
            if (nombre.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "El nombre es obligatorio.").showAndWait();
                return;
            }

            String referencia = txtReferencia.getText() != null ? txtReferencia.getText().trim() : "";
            String categoria = txtCategoria.getText() != null ? txtCategoria.getText().trim() : "";
            String descripcion = txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : "";

            Integer stock = parseIntOrDefault(txtStock.getText(), 0, "El stock debe ser numérico.");
            Integer stockMinimo = parseIntOrDefault(txtStockMinimo.getText(), 0, "El stock mínimo debe ser numérico.");
            BigDecimal precioCompra = parseDecimalOrDefault(txtPrecioCompra.getText(), "El precio de compra no es válido.");
            BigDecimal precioVenta = parseDecimalOrDefault(txtPrecioVenta.getText(), "El precio de venta no es válido.");

            SaveProductRequest request = new SaveProductRequest(editingId, empresaId, nombre, descripcion, referencia,
                    categoria, stock, stockMinimo, precioCompra, precioVenta);
            saveProductUseCase.save(request);

            saved = true;
            closeWindow();

        } catch (IllegalArgumentException ex) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Datos inválidos");
            alert.setHeaderText(null);
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo guardar el producto");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onCancelar() {
        saved = false;
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
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
            if (newV == null) {
                return;
            }

            internalChange[0] = true;
            combo.getEditor().setText(newV.nombre);
            combo.setValue(newV);
            internalChange[0] = false;
        });

        combo.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (internalChange[0]) {
                return;
            }

            String f = (newText == null ? "" : newText).toLowerCase(Locale.ROOT).trim();
            empresasFiltradas.setPredicate(opt -> f.isEmpty() || (opt.nombre != null && opt.nombre.toLowerCase(Locale.ROOT).contains(f)));

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

    private Integer parseIntOrDefault(String text, int defaultValue, String errorMessage) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private BigDecimal parseDecimalOrDefault(String text, String errorMessage) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String normalized = trimmed.replace(',', '.');
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String formatInt(Integer value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private String formatDecimal(BigDecimal value) {
        return value == null ? "0.00" : value.toPlainString();
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
