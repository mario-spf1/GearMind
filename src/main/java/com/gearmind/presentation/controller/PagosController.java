package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.payment.ListPaymentsUseCase;
import com.gearmind.domain.payment.Payment;
import com.gearmind.domain.payment.PaymentStatus;
import com.gearmind.domain.payment.PaymentType;
import com.gearmind.infrastructure.payment.MySqlPaymentRepository;
import com.gearmind.presentation.table.SmartTable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PagosController {

    @FXML
    private TableView<Payment> tblPagos;
    @FXML
    private TableColumn<Payment, String> colEmpresa;
    @FXML
    private TableColumn<Payment, String> colCliente;
    @FXML
    private TableColumn<Payment, String> colFactura;
    @FXML
    private TableColumn<Payment, String> colTipo;
    @FXML
    private TableColumn<Payment, String> colEstado;
    @FXML
    private TableColumn<Payment, String> colTotal;
    @FXML
    private TableColumn<Payment, String> colPagado;
    @FXML
    private TableColumn<Payment, String> colPendiente;
    @FXML
    private TableColumn<Payment, String> colPlazos;
    @FXML
    private TableColumn<Payment, String> colInteres;
    @FXML
    private TableColumn<Payment, Payment> colAcciones;

    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Label lblHeaderInfo;
    @FXML
    private Label lblResumen;
    @FXML
    private TextField filterClienteField;
    @FXML
    private TextField filterFacturaField;
    @FXML
    private ComboBox<String> filterEstadoCombo;
    @FXML
    private ComboBox<String> filterEmpresaCombo;
    @FXML
    private HBox boxFilterEmpresa;

    private final ObservableList<Payment> masterData = FXCollections.observableArrayList();
    private SmartTable<Payment> smartTable;

    private final ListPaymentsUseCase listPaymentsUseCase;
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.getDefault()));

    public PagosController() {
        this.listPaymentsUseCase = new ListPaymentsUseCase(new MySqlPaymentRepository());
    }

    @FXML
    private void initialize() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        tblPagos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tblPagos.setPlaceholder(new Label("No hay pagos que mostrar."));

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
        colFactura.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getFacturaNumero())));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(mapTypeToLabel(c.getValue().getTipo())));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-table-badge", "tfx-badge-success", "tfx-badge-warning");
                if (empty || item == null) {
                    setText(null);
                    setAlignment(javafx.geometry.Pos.CENTER);
                } else {
                    setText(item);
                    getStyleClass().add("tfx-badge");
                    getStyleClass().addAll("tfx-badge", "tfx-table-badge");
                    getStyleClass().add("Contado".equalsIgnoreCase(item) ? "tfx-badge-success" : "tfx-badge-warning");
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(mapStatusToLabel(c.getValue().getEstado())));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-table-badge", "tfx-badge-success", "tfx-badge-warning");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().addAll("tfx-badge", "tfx-table-badge");
                    getStyleClass().add("Completado".equalsIgnoreCase(item) ? "tfx-badge-success" : "tfx-badge-warning");
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getTotal())));
        colPagado.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getTotalPagado())));
        colPendiente.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getTotalPendiente())));
        colPlazos.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getNumeroPlazos() != null ? c.getValue().getNumeroPlazos() : 0)));
        colInteres.setCellValueFactory(c -> new SimpleStringProperty(formatInterest(c.getValue().getInteresPorcentaje())));
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnGestionar = new Button("Gestionar");
            private final HBox box = new HBox(8, btnGestionar);

            {
                box.getStyleClass().add("tfx-table-actions");
                btnGestionar.getStyleClass().add("tfx-table-action-btn");
                btnGestionar.setTooltip(new javafx.scene.control.Tooltip("Gestionar pago"));
                btnGestionar.setOnAction(e -> {
                    Payment payment = getItem();
                    if (payment != null) {
                        openPagoPlazos(payment);
                    }
                });
            }

            @Override
            protected void updateItem(Payment payment, boolean empty) {
                super.updateItem(payment, empty);
                if (empty || payment == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
        colAcciones.setSortable(false);

        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(10, 25, 50, 100));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(25));
        }

        smartTable = new SmartTable<>(tblPagos, masterData, null, cmbPageSize, lblResumen, "pagos", null);

        tblPagos.setFixedCellSize(28);

        smartTable.addColumnFilter(filterClienteField, (p, text) -> safe(p.getClienteNombre()).contains(text));
        smartTable.addColumnFilter(filterFacturaField, (p, text) -> safe(p.getFacturaNumero()).contains(text));

        if (filterEstadoCombo != null) {
            filterEstadoCombo.setItems(FXCollections.observableArrayList("Todos", "Abierto", "Completado"));
            filterEstadoCombo.getSelectionModel().select("Todos");

            smartTable.addColumnFilter(filterEstadoCombo, (p, selected) -> {
                if (selected == null || "Todos".equalsIgnoreCase(selected)) {
                    return true;
                }
                return mapStatusToLabel(p.getEstado()).equalsIgnoreCase(selected);
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
        loadPaymentsFromDb();
    }

    @FXML
    private void onRefrescar() {
        loadPaymentsFromDb();
    }

    @FXML
    private void onLimpiarFiltros() {
        if (filterClienteField != null) {
            filterClienteField.clear();
        }
        if (filterFacturaField != null) {
            filterFacturaField.clear();
        }
        if (filterEstadoCombo != null) {
            filterEstadoCombo.getSelectionModel().select("Todos");
        }
        if (AuthContext.isSuperAdmin() && filterEmpresaCombo != null) {
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }
    }

    private void loadPaymentsFromDb() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();
        List<Payment> payments;

        if (isSuperAdmin) {
            payments = listPaymentsUseCase.listAllWithEmpresa();
        } else {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            payments = listPaymentsUseCase.listByEmpresa(empresaId);
        }

        payments.sort(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        masterData.setAll(payments);

        if (isSuperAdmin && filterEmpresaCombo != null) {
            var empresas = payments.stream().map(Payment::getEmpresaNombre).filter(s -> s != null && !s.isBlank()).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            filterEmpresaCombo.setItems(FXCollections.observableArrayList(empresas));
            filterEmpresaCombo.getItems().add(0, "Todas");
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }

        smartTable.refresh();

        if (lblHeaderInfo != null) {
            lblHeaderInfo.setText(masterData.size() + " pagos registrados");
        }
    }

    private void setupRowDoubleClick() {
        tblPagos.setRowFactory(tv -> {
            TableRow<Payment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openPagoPlazos(row.getItem());
                }
            });
            return row;
        });
    }

    private void openPagoPlazos(Payment payment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/PagoPlazosView.fxml"));
            Parent root = loader.load();
            PagoPlazosController controller = loader.getController();
            controller.initForPayment(payment.getId());

            Stage stage = new Stage();
            stage.setTitle("Gestionar pago");
            stage.initOwner(tblPagos.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            loadPaymentsFromDb();
        } catch (Exception ex) {
            ex.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "No se pudo abrir la gestión de pagos: " + ex.getMessage()).showAndWait();
        }
    }

    private String formatPrice(BigDecimal value) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        return priceFormat.format(resolved) + " €";
    }

    private String formatInterest(BigDecimal value) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        return resolved.stripTrailingZeros().toPlainString() + " %";
    }

    private String mapStatusToLabel(PaymentStatus status) {
        if (status == null) {
            return "Abierto";
        }
        return switch (status) {
            case ABIERTO ->
                "Abierto";
            case COMPLETADO ->
                "Completado";
        };
    }

    private String mapTypeToLabel(PaymentType type) {
        if (type == null) {
            return "Contado";
        }
        return switch (type) {
            case CONTADO ->
                "Contado";
            case PLAZOS ->
                "A plazos";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.getDefault());
    }

    private String safeRaw(String value) {
        return value == null ? "" : value;
    }
}
