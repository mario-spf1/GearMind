package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.product.ActivateProductUseCase;
import com.gearmind.application.product.DeactivateProductUseCase;
import com.gearmind.application.product.ListProductsUseCase;
import com.gearmind.domain.product.Product;
import com.gearmind.infrastructure.product.MySqlProductRepository;
import com.gearmind.presentation.table.SmartTable;
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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ProductosController {

    @FXML
    private TableView<Product> tblProductos;
    @FXML
    private TableColumn<Product, String> colEmpresa;
    @FXML
    private TableColumn<Product, String> colNombre;
    @FXML
    private TableColumn<Product, String> colReferencia;
    @FXML
    private TableColumn<Product, String> colCategoria;
    @FXML
    private TableColumn<Product, String> colDescripcion;
    @FXML
    private TableColumn<Product, Integer> colStock;
    @FXML
    private TableColumn<Product, Integer> colStockMinimo;
    @FXML
    private TableColumn<Product, String> colPrecioCompra;
    @FXML
    private TableColumn<Product, String> colPrecioVenta;
    @FXML
    private TableColumn<Product, String> colEstado;
    @FXML
    private TableColumn<Product, Product> colAcciones;

    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Button btnNuevoProducto;
    @FXML
    private Label lblHeaderInfo;

    @FXML
    private TextField filterNombreField;
    @FXML
    private TextField filterReferenciaField;
    @FXML
    private TextField filterCategoriaField;
    @FXML
    private ComboBox<String> filterEstadoCombo;

    @FXML
    private ComboBox<String> filterEmpresaCombo;
    @FXML
    private HBox boxFilterEmpresa;
    @FXML
    private HBox boxFilterEstado;

    @FXML
    private Label lblResumen;

    private final ListProductsUseCase listProductsUseCase;
    private final DeactivateProductUseCase deactivateProductUseCase;
    private final ActivateProductUseCase activateProductUseCase;

    private final ObservableList<Product> masterData = FXCollections.observableArrayList();
    private SmartTable<Product> smartTable;

    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.getDefault()));

    public ProductosController() {
        MySqlProductRepository repo = new MySqlProductRepository();
        this.listProductsUseCase = new ListProductsUseCase(repo);
        this.deactivateProductUseCase = new DeactivateProductUseCase(repo);
        this.activateProductUseCase = new ActivateProductUseCase(repo);
    }

    @FXML
    private void initialize() {

        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        tblProductos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblProductos.setPlaceholder(new Label("No hay productos que mostrar."));

        if (!isSuperAdmin) {
            if (colEmpresa != null) {
                colEmpresa.setVisible(false);
            }
            if (boxFilterEmpresa != null) {
                boxFilterEmpresa.setVisible(false);
                boxFilterEmpresa.setManaged(false);
            }
        } else {
            if (colEmpresa != null) {
                colEmpresa.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getEmpresaNombre())));
            }
        }

        colNombre.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nombre"));
        colReferencia.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("referencia"));
        colCategoria.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("categoria"));

        colDescripcion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescripcion()));
        colDescripcion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String trimmed = item.length() > 40 ? item.substring(0, 37) + "..." : item;
                    setText(trimmed);
                    setTooltip(new Tooltip(item));
                }
            }
        });

        colStock.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStock()));
        colStockMinimo.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStockMinimo()));
        colPrecioCompra.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getPrecioCompra())));
        colPrecioVenta.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getPrecioVenta())));

        colEstado.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-badge-success", "tfx-badge-danger");

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().add("tfx-badge");
                    getStyleClass().add("Activo".equalsIgnoreCase(item) ? "tfx-badge-success" : "tfx-badge-danger");
                }
            }
        });

        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {

            private final Button btnEditar = new Button("Editar");
            private final Button btnToggle = new Button();
            private final HBox box = new HBox(8, btnEditar, btnToggle);

            {
                btnEditar.getStyleClass().add("tfx-icon-btn");
                btnToggle.getStyleClass().add("tfx-icon-btn");

                btnEditar.setTooltip(new Tooltip("Editar producto"));
                btnToggle.setTooltip(new Tooltip("Activar/Desactivar"));

                btnEditar.setOnAction(e -> {
                    Product p = getItem();
                    if (p != null) {
                        openProductoForm(p);
                    }
                });

                btnToggle.setOnAction(e -> {
                    Product p = getItem();
                    if (p != null) {
                        toggleProductActive(p);
                    }
                });
            }

            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setGraphic(null);
                    return;
                }

                btnToggle.getStyleClass().removeAll("tfx-icon-btn-danger", "tfx-icon-btn-success");

                if (product.isActivo()) {
                    btnToggle.setText("Desactivar");
                    btnToggle.getStyleClass().add("tfx-icon-btn-danger");
                    btnToggle.setTooltip(new Tooltip("Desactivar producto"));
                } else {
                    btnToggle.setText("Activar");
                    btnToggle.getStyleClass().add("tfx-icon-btn-success");
                    btnToggle.setTooltip(new Tooltip("Activar producto"));
                }

                setGraphic(box);
            }
        });
        colAcciones.setSortable(false);

        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(5, 15, 25, 0));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(15));

            var converter = new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer value) {
                    if (value == null) {
                        return "";
                    }
                    return value == 0 ? "Todos" : String.valueOf(value);
                }

                @Override
                public Integer fromString(String s) {
                    if (s == null) {
                        return 15;
                    }
                    s = s.trim();
                    return "Todos".equalsIgnoreCase(s) ? 0 : Integer.valueOf(s);
                }
            };

            cmbPageSize.setConverter(converter);
            cmbPageSize.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : (item == 0 ? "Todos" : String.valueOf(item)));
                }
            });
            cmbPageSize.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : (item == 0 ? "Todos" : String.valueOf(item)));
                }
            });
        }

        smartTable = new SmartTable<>(tblProductos, masterData, null, cmbPageSize, lblResumen, "productos", null);

        tblProductos.setFixedCellSize(28);
        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblProductos.getFixedCellSize() + 2;
            tblProductos.setPrefHeight(tableHeight);
            tblProductos.setMinHeight(Region.USE_PREF_SIZE);
        });

        smartTable.addColumnFilter(filterNombreField, (p, text) -> safe(p.getNombre()).contains(text));
        smartTable.addColumnFilter(filterReferenciaField, (p, text) -> safe(p.getReferencia()).contains(text));
        smartTable.addColumnFilter(filterCategoriaField, (p, text) -> safe(p.getCategoria()).contains(text));

        if (filterEstadoCombo != null) {
            filterEstadoCombo.setItems(FXCollections.observableArrayList("Todos", "Activo", "Inactivo"));
            filterEstadoCombo.getSelectionModel().select("Todos");

            smartTable.addColumnFilter(filterEstadoCombo, (p, selected) -> {
                if (selected == null || "Todos".equalsIgnoreCase(selected)) {
                    return true;
                }
                return "Activo".equalsIgnoreCase(selected) ? p.isActivo() : !p.isActivo();
            });
        }

        if (isSuperAdmin && filterEmpresaCombo != null) {
            smartTable.addColumnFilter(filterEmpresaCombo, (p, selected) -> {
                if (selected == null || "Todas".equalsIgnoreCase(selected)) {
                    return true;
                }
                return safeRaw(p.getEmpresaNombre()).equalsIgnoreCase(selected);
            });
        }

        setupRowDoubleClick();
        loadProductosFromDb();
    }

    private void loadProductosFromDb() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        List<Product> productos;
        if (isSuperAdmin) {
            productos = listProductsUseCase.listAllWithEmpresa();
        } else {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            productos = listProductsUseCase.listByEmpresa(empresaId);
        }

        productos.sort(Comparator.comparing(Product::getNombre, String.CASE_INSENSITIVE_ORDER));
        masterData.setAll(productos);

        if (isSuperAdmin && filterEmpresaCombo != null) {
            var empresas = productos.stream()
                    .map(Product::getEmpresaNombre)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();

            filterEmpresaCombo.setItems(FXCollections.observableArrayList(empresas));
            filterEmpresaCombo.getItems().add(0, "Todas");
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }

        smartTable.refresh();

        if (lblHeaderInfo != null) {
            lblHeaderInfo.setText(masterData.size() + " productos registrados");
        }
    }

    private void setupRowDoubleClick() {
        tblProductos.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openProductoForm(row.getItem());
                }
            });
            return row;
        });
    }

    private void toggleProductActive(Product product) {
        if (product.isActivo()) {
            deactivateProduct(product);
        } else {
            activateProduct(product);
        }
    }

    private void deactivateProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Desactivar producto");
        alert.setHeaderText("¿Desactivar producto?");
        alert.setContentText("El producto \"" + product.getNombre() + "\" dejará de estar disponible en los listados.");

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                deactivateProductUseCase.deactivate(product.getId(), product.getEmpresaId());
                loadProductosFromDb();
            }
        });
    }

    private void activateProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Activar producto");
        alert.setHeaderText("¿Activar producto?");
        alert.setContentText("El producto \"" + product.getNombre() + "\" volverá a estar disponible en los listados.");

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                activateProductUseCase.activate(product.getId(), product.getEmpresaId());
                loadProductosFromDb();
            }
        });
    }

    @FXML
    private void onNuevoProducto() {
        openProductoForm(null);
    }

    @FXML
    private void onRefrescar() {
        loadProductosFromDb();
    }

    private void openProductoForm(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProductoFormView.fxml"));
            Parent root = loader.load();

            ProductoFormController controller = loader.getController();
            if (product == null) {
                controller.initForNew();
            } else {
                controller.initForEdit(product);
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(java.util.Objects.requireNonNull(getClass().getResource("/styles/theme.css")).toExternalForm());
            scene.getStylesheets().add(java.util.Objects.requireNonNull(getClass().getResource("/styles/components.css")).toExternalForm());
            Stage stage = new Stage();
            stage.initOwner(tblProductos.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(product == null ? "Nuevo producto" : "Editar producto");
            stage.setResizable(false);
            stage.setScene(scene);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadProductosFromDb();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario.\n\n" + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onLimpiarFiltros() {
        if (filterNombreField != null) {
            filterNombreField.clear();
        }
        if (filterReferenciaField != null) {
            filterReferenciaField.clear();
        }
        if (filterCategoriaField != null) {
            filterCategoriaField.clear();
        }

        if (filterEstadoCombo != null) {
            filterEstadoCombo.getSelectionModel().select("Todos");
        }

        if (AuthContext.isSuperAdmin() && filterEmpresaCombo != null) {
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }

        smartTable.refresh();
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private String safeRaw(String s) {
        return s == null ? "" : s;
    }

    private String formatPrice(BigDecimal value) {
        return value == null ? "0.00" : priceFormat.format(value);
    }
}
