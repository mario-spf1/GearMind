package com.gearmind.presentation.controller;

import com.gearmind.application.payment.GetPaymentUseCase;
import com.gearmind.application.payment.ListPaymentInstallmentsUseCase;
import com.gearmind.application.payment.ListPaymentRecordsUseCase;
import com.gearmind.application.payment.RegisterInstallmentPaymentUseCase;
import com.gearmind.domain.payment.Payment;
import com.gearmind.domain.payment.PaymentInstallment;
import com.gearmind.domain.payment.PaymentInstallmentStatus;
import com.gearmind.domain.payment.PaymentRecord;
import com.gearmind.domain.payment.PaymentStatus;
import com.gearmind.domain.payment.PaymentType;
import com.gearmind.infrastructure.invoice.MySqlInvoiceRepository;
import com.gearmind.infrastructure.payment.MySqlPaymentRepository;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PagoPlazosController {

    @FXML
    private Label lblFactura;
    @FXML
    private Label lblCliente;
    @FXML
    private Label lblTipo;
    @FXML
    private Label lblEstado;
    @FXML
    private Label lblTotal;
    @FXML
    private Label lblPagado;
    @FXML
    private Label lblPendiente;
    @FXML
    private Label lblPlazos;

    @FXML
    private TableView<PaymentInstallment> tblPlazos;
    @FXML
    private TableColumn<PaymentInstallment, String> colNumero;
    @FXML
    private TableColumn<PaymentInstallment, String> colVencimiento;
    @FXML
    private TableColumn<PaymentInstallment, String> colImporte;
    @FXML
    private TableColumn<PaymentInstallment, String> colEstado;
    @FXML
    private TableColumn<PaymentInstallment, String> colFechaPago;
    @FXML
    private TableColumn<PaymentInstallment, PaymentInstallment> colAcciones;

    @FXML
    private TableView<PaymentRecord> tblRegistros;
    @FXML
    private TableColumn<PaymentRecord, String> colRegistroFecha;
    @FXML
    private TableColumn<PaymentRecord, String> colRegistroImporte;
    @FXML
    private TableColumn<PaymentRecord, String> colRegistroObs;

    private final GetPaymentUseCase getPaymentUseCase;
    private final ListPaymentInstallmentsUseCase listInstallmentsUseCase;
    private final ListPaymentRecordsUseCase listRecordsUseCase;
    private final RegisterInstallmentPaymentUseCase registerInstallmentPaymentUseCase;

    private final ObservableList<PaymentInstallment> installments = FXCollections.observableArrayList();
    private final ObservableList<PaymentRecord> records = FXCollections.observableArrayList();

    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.getDefault()));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private long paymentId;

    public PagoPlazosController() {
        MySqlPaymentRepository paymentRepository = new MySqlPaymentRepository();
        this.getPaymentUseCase = new GetPaymentUseCase(paymentRepository);
        this.listInstallmentsUseCase = new ListPaymentInstallmentsUseCase(paymentRepository);
        this.listRecordsUseCase = new ListPaymentRecordsUseCase(paymentRepository);
        this.registerInstallmentPaymentUseCase = new RegisterInstallmentPaymentUseCase(paymentRepository, new MySqlInvoiceRepository());
    }

    public void initForPayment(long pagoId) {
        this.paymentId = pagoId;
        loadData();
    }

    @FXML
    private void initialize() {
        tblPlazos.setItems(installments);
        tblPlazos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblPlazos.setFixedCellSize(28);
        tblRegistros.setItems(records);
        tblRegistros.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblRegistros.setFixedCellSize(28);

        colNumero.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getNumero() != null ? c.getValue().getNumero() : 0)));
        colVencimiento.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getFechaVencimiento())));
        colImporte.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getImporte())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(mapInstallmentStatusToLabel(c.getValue().getEstado())));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-table-badge", "tfx-badge-success", "tfx-badge-warning");
                if (empty || item == null) {
                    setText(null);
                    setAlignment(javafx.geometry.Pos.CENTER);
                } else {
                    setText(item);
                    getStyleClass().addAll("tfx-badge", "tfx-table-badge");
                    getStyleClass().add("Pagado".equalsIgnoreCase(item) ? "tfx-badge-success" : "tfx-badge-warning");
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        colFechaPago.setCellValueFactory(c -> new SimpleStringProperty(formatDateTime(c.getValue().getFechaPago())));

        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnPagar = new Button("Registrar");
            private final HBox box = new HBox(8, btnPagar);

            {
                box.getStyleClass().add("tfx-table-actions");
                btnPagar.getStyleClass().add("tfx-table-action-btn");
                btnPagar.setTooltip(new javafx.scene.control.Tooltip("Registrar pago"));
                btnPagar.setOnAction(e -> {
                    PaymentInstallment installment = getItem();
                    if (installment != null) {
                        registerPayment(installment);
                    }
                });
            }

            @Override
            protected void updateItem(PaymentInstallment installment, boolean empty) {
                super.updateItem(installment, empty);
                if (empty || installment == null) {
                    setGraphic(null);
                    return;
                }
                boolean canPay = installment.getEstado() != PaymentInstallmentStatus.PAGADO;
                btnPagar.setDisable(!canPay);
                setGraphic(box);
            }
        });
        colAcciones.setSortable(false);

        colRegistroFecha.setCellValueFactory(c -> new SimpleStringProperty(formatDateTime(c.getValue().getFecha())));
        colRegistroImporte.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getImporte())));
        colRegistroObs.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getObservaciones())));
        installments.addListener((ListChangeListener<PaymentInstallment>) change -> updateTablesHeight());
        records.addListener((ListChangeListener<PaymentRecord>) change -> updateTablesHeight());
        updateTablesHeight();
    }

    @FXML
    private void onCerrar() {
        Stage stage = (Stage) tblPlazos.getScene().getWindow();
        stage.close();
    }

    private void loadData() {
        Payment payment = getPaymentUseCase.execute(paymentId).orElse(null);
        if (payment == null) {
            new Alert(Alert.AlertType.WARNING, "No se encontró el pago.").showAndWait();
            return;
        }

        if (lblFactura != null) {
            lblFactura.setText(payment.getFacturaNumero());
        }
        if (lblCliente != null) {
            lblCliente.setText(payment.getClienteNombre());
        }
        if (lblTipo != null) {
            lblTipo.setText(mapTypeToLabel(payment.getTipo()));
        }
        if (lblEstado != null) {
            lblEstado.setText(mapStatusToLabel(payment.getEstado()));
        }
        if (lblTotal != null) {
            lblTotal.setText(formatPrice(payment.getTotal()));
        }
        if (lblPagado != null) {
            lblPagado.setText(formatPrice(payment.getTotalPagado()));
        }
        if (lblPendiente != null) {
            lblPendiente.setText(formatPrice(payment.getTotalPendiente()));
        }
        if (lblPlazos != null) {
            lblPlazos.setText(String.valueOf(payment.getNumeroPlazos() != null ? payment.getNumeroPlazos() : 0));
        }

        installments.setAll(listInstallmentsUseCase.execute(paymentId));
        records.setAll(listRecordsUseCase.execute(paymentId));
    }

    private void registerPayment(PaymentInstallment installment) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Registrar pago");
        dialog.setHeaderText("Registrar pago del plazo #" + installment.getNumero());
        dialog.setContentText("Observaciones (opcional):");

        dialog.showAndWait().ifPresent(obs -> {
            try {
                registerInstallmentPaymentUseCase.execute(paymentId, installment.getId(), obs);
                loadData();
                new Alert(Alert.AlertType.INFORMATION, "Pago registrado correctamente.").showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "No se pudo registrar el pago: " + ex.getMessage()).showAndWait();
            }
        });
    }

    private String formatPrice(BigDecimal value) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        return priceFormat.format(resolved) + " €";
    }

    private void updateTablesHeight() {
        updateTableHeight(tblPlazos, installments.size(), 8);
        updateTableHeight(tblRegistros, records.size(), 6);
        Platform.runLater(() -> {
            if (tblPlazos.getScene() != null && tblPlazos.getScene().getWindow() instanceof Stage stage) {
                stage.sizeToScene();
            }
        });
    }

    private void updateTableHeight(TableView<?> table, int rowCount, int maxVisibleRows) {
        if (table == null) {
            return;
        }
        int rows = Math.max(1, Math.min(rowCount, maxVisibleRows));
        double headerHeight = 28;
        double tableHeight = headerHeight + rows * table.getFixedCellSize() + 2;
        table.setPrefHeight(tableHeight);
        table.setMinHeight(Region.USE_PREF_SIZE);
        table.setMaxHeight(Region.USE_PREF_SIZE);
    }

    private String formatDate(LocalDate value) {
        if (value == null) {
            return "";
        }
        return value.format(dateFormatter);
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(dateTimeFormatter);
    }

    private String mapInstallmentStatusToLabel(PaymentInstallmentStatus status) {
        if (status == null) {
            return "Pendiente";
        }
        return switch (status) {
            case PENDIENTE ->
                "Pendiente";
            case PAGADO ->
                "Pagado";
        };
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

    private String safeRaw(String value) {
        return value == null ? "" : value;
    }
}
