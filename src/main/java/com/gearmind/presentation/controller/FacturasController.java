package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.invoice.ListInvoicesUseCase;
import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceStatus;
import com.gearmind.infrastructure.invoice.InvoicePdfStorage;
import com.gearmind.infrastructure.invoice.MySqlInvoiceRepository;
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

import java.awt.Desktop;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FacturasController {

    @FXML
    private TableView<Invoice> tblFacturas;
    @FXML
    private TableColumn<Invoice, String> colEmpresa;
    @FXML
    private TableColumn<Invoice, String> colCliente;
    @FXML
    private TableColumn<Invoice, String> colVehiculo;
    @FXML
    private TableColumn<Invoice, String> colNumero;
    @FXML
    private TableColumn<Invoice, String> colFecha;
    @FXML
    private TableColumn<Invoice, String> colEstado;
    @FXML
    private TableColumn<Invoice, String> colTotal;
    @FXML
    private TableColumn<Invoice, Invoice> colAcciones;
    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Button btnNuevaFactura;
    @FXML
    private Label lblHeaderInfo;
    @FXML
    private Label lblResumen;
    @FXML
    private TextField filterClienteField;
    @FXML
    private TextField filterNumeroField;
    @FXML
    private ComboBox<String> filterEstadoCombo;
    @FXML
    private ComboBox<String> filterEmpresaCombo;
    @FXML
    private HBox boxFilterEmpresa;

    private final ObservableList<Invoice> masterData = FXCollections.observableArrayList();
    private SmartTable<Invoice> smartTable;

    private final ListInvoicesUseCase listInvoicesUseCase;
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.getDefault()));

    public FacturasController() {
        this.listInvoicesUseCase = new ListInvoicesUseCase(new MySqlInvoiceRepository());
    }

    @FXML
    private void initialize() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        tblFacturas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblFacturas.setPlaceholder(new Label("No hay facturas que mostrar."));

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

        colCliente.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getClienteNombre())));
        colVehiculo.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getVehiculoEtiqueta())));
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getNumero())));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getFecha())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(mapStatusToLabel(c.getValue().getEstado())));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-badge-success", "tfx-badge-warning", "tfx-badge-danger");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().add("tfx-badge");
                    switch (item) {
                        case "Pagada" ->
                            getStyleClass().add("tfx-badge-success");
                        case "Pendiente" ->
                            getStyleClass().add("tfx-badge-warning");
                        case "Anulada" ->
                            getStyleClass().add("tfx-badge-danger");
                        default -> {
                        }
                    }
                }
            }
        });
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getTotal())));

        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnPdf = new Button("PDF");
            private final HBox box = new HBox(8, btnEditar, btnPdf);

            {
                btnEditar.getStyleClass().add("tfx-icon-btn");
                btnPdf.getStyleClass().add("tfx-icon-btn-secondary");

                btnEditar.setOnAction(e -> {
                    Invoice invoice = getItem();
                    if (invoice != null) {
                        openFacturaForm(invoice);
                    }
                });

                btnPdf.setOnAction(e -> {
                    Invoice invoice = getItem();
                    if (invoice != null) {
                        openPdf(invoice.getId());
                    }
                });
            }

            @Override
            protected void updateItem(Invoice invoice, boolean empty) {
                super.updateItem(invoice, empty);
                setGraphic(empty || invoice == null ? null : box);
            }
        });
        colAcciones.setSortable(false);

        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(10, 25, 50, 100));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(25));
        }

        smartTable = new SmartTable<>(tblFacturas, masterData, null, cmbPageSize, lblResumen, "facturas", null);

        tblFacturas.setFixedCellSize(28);
        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblFacturas.getFixedCellSize() + 2;
            tblFacturas.setPrefHeight(tableHeight);
            tblFacturas.setMinHeight(Region.USE_PREF_SIZE);
        });

        smartTable.addColumnFilter(filterClienteField, (b, text) -> safe(b.getClienteNombre()).contains(text));
        smartTable.addColumnFilter(filterNumeroField, (b, text) -> safe(b.getNumero()).contains(text));

        if (filterEstadoCombo != null) {
            filterEstadoCombo.setItems(FXCollections.observableArrayList("Todos", "Pendiente", "Pagada", "Anulada", "Borrador"));
            filterEstadoCombo.getSelectionModel().select("Todos");

            smartTable.addColumnFilter(filterEstadoCombo, (b, selected) -> {
                if (selected == null || "Todos".equalsIgnoreCase(selected)) {
                    return true;
                }
                return mapStatusToLabel(b.getEstado()).equalsIgnoreCase(selected);
            });
        }

        if (isSuperAdmin && filterEmpresaCombo != null) {
            smartTable.addColumnFilter(filterEmpresaCombo, (b, selected) -> {
                if (selected == null || "Todas".equalsIgnoreCase(selected)) {
                    return true;
                }
                return safeRaw(b.getEmpresaNombre()).equalsIgnoreCase(selected);
            });
        }

        setupRowDoubleClick();
        loadInvoicesFromDb();
    }

    @FXML
    private void onRefrescar() {
        loadInvoicesFromDb();
    }

    @FXML
    private void onNuevaFactura() {
        openFacturaForm(null);
    }

    @FXML
    private void onLimpiarFiltros() {
        if (filterClienteField != null) {
            filterClienteField.clear();
        }
        if (filterNumeroField != null) {
            filterNumeroField.clear();
        }
        if (filterEstadoCombo != null) {
            filterEstadoCombo.getSelectionModel().select("Todos");
        }
        if (AuthContext.isSuperAdmin() && filterEmpresaCombo != null) {
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }
    }

    private void loadInvoicesFromDb() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();
        List<Invoice> invoices;

        if (isSuperAdmin) {
            invoices = listInvoicesUseCase.listAllWithEmpresa();
        } else {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            invoices = listInvoicesUseCase.listByEmpresa(empresaId);
        }

        invoices.sort(Comparator.comparing(Invoice::getFecha, Comparator.nullsLast(Comparator.reverseOrder())));
        masterData.setAll(invoices);

        if (isSuperAdmin && filterEmpresaCombo != null) {
            var empresas = invoices.stream().map(Invoice::getEmpresaNombre).filter(s -> s != null && !s.isBlank()).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            filterEmpresaCombo.setItems(FXCollections.observableArrayList(empresas));
            filterEmpresaCombo.getItems().add(0, "Todas");
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }

        smartTable.refresh();

        if (lblHeaderInfo != null) {
            lblHeaderInfo.setText(masterData.size() + " facturas registradas");
        }
    }

    private void setupRowDoubleClick() {
        tblFacturas.setRowFactory(tv -> {
            TableRow<Invoice> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openFacturaForm(row.getItem());
                }
            });
            return row;
        });
    }

    private void openFacturaForm(Invoice invoice) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/FacturaFormView.fxml"));
            Parent root = loader.load();
            FacturaFormController controller = loader.getController();
            if (invoice == null) {
                controller.initForNew();
            } else {
                controller.initForEdit(invoice.getId());
            }

            Stage stage = new Stage();
            stage.setTitle(invoice == null ? "Nueva factura" : "Editar factura");
            stage.initOwner(tblFacturas.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadInvoicesFromDb();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario de facturas: " + ex.getMessage()).showAndWait();
        }
    }

    private void openPdf(long invoiceId) {
        Path path = InvoicePdfStorage.resolvePath(invoiceId);
        if (!Files.exists(path)) {
            new Alert(Alert.AlertType.WARNING, "No se encontró el PDF. Genera la factura para crear el archivo.").showAndWait();
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(path.toFile());
            } else {
                new Alert(Alert.AlertType.WARNING, "No se pudo abrir el PDF automáticamente. Ruta: " + path).showAndWait();
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el PDF: " + ex.getMessage()).showAndWait();
        }
    }

    private String formatPrice(BigDecimal value) {
        if (value == null) {
            return "0,00";
        }
        return priceFormat.format(value);
    }

    private String formatDate(java.time.LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
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

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.getDefault());
    }

    private String safeRaw(String value) {
        return value == null ? "" : value;
    }
}
