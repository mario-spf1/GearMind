package com.gearmind.presentation.controller;

import com.gearmind.application.invoice.GetInvoiceUseCase;
import com.gearmind.application.payment.CreatePaymentRequest;
import com.gearmind.application.payment.CreatePaymentUseCase;
import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.payment.PaymentType;
import com.gearmind.infrastructure.invoice.MySqlInvoiceRepository;
import com.gearmind.infrastructure.payment.MySqlPaymentRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

public class PagoFormController {

    @FXML
    private Label lblFactura;
    @FXML
    private Label lblCliente;
    @FXML
    private Label lblTotal;
    @FXML
    private RadioButton rbContado;
    @FXML
    private RadioButton rbPlazos;
    @FXML
    private TextField txtPlazos;
    @FXML
    private TextField txtInteres;
    @FXML
    private DatePicker dpPrimerVencimiento;
    @FXML
    private Button btnGuardar;

    private final GetInvoiceUseCase getInvoiceUseCase;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.getDefault()));

    private boolean saved = false;
    private Invoice invoice;

    public PagoFormController() {
        MySqlInvoiceRepository invoiceRepository = new MySqlInvoiceRepository();
        this.getInvoiceUseCase = new GetInvoiceUseCase(invoiceRepository);
        this.createPaymentUseCase = new CreatePaymentUseCase(new MySqlPaymentRepository(), invoiceRepository);
    }

    public boolean isSaved() {
        return saved;
    }

    public void initForInvoice(long facturaId) {
        invoice = getInvoiceUseCase.execute(facturaId).orElse(null);
        if (invoice == null) {
            new Alert(Alert.AlertType.WARNING, "No se encontró la factura.").showAndWait();
            close();
            return;
        }

        if (lblFactura != null) {
            lblFactura.setText(invoice.getNumero());
        }
        if (lblCliente != null) {
            lblCliente.setText(invoice.getClienteNombre());
        }
        if (lblTotal != null) {
            lblTotal.setText(formatPrice(invoice.getTotal()));
        }
        if (rbContado != null) {
            rbContado.setSelected(true);
        }
        if (dpPrimerVencimiento != null) {
            dpPrimerVencimiento.setValue(LocalDate.now());
        }
        if (txtInteres != null) {
            txtInteres.setText("0");
        }
        updateInstallmentFields();
        validateForm();
    }

    @FXML
    private void initialize() {
        ToggleGroup group = new ToggleGroup();
        if (rbContado != null) {
            rbContado.setToggleGroup(group);
        }
        if (rbPlazos != null) {
            rbPlazos.setToggleGroup(group);
        }

        group.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            updateInstallmentFields();
            validateForm();
        });

        if (txtPlazos != null) {
            txtPlazos.textProperty().addListener((obs, oldValue, newValue) -> validateForm());
        }
        if (dpPrimerVencimiento != null) {
            dpPrimerVencimiento.valueProperty().addListener((obs, oldValue, newValue) -> validateForm());
        }
        if (txtInteres != null) {
            txtInteres.textProperty().addListener((obs, oldValue, newValue) -> validateForm());
        }
    }

    @FXML
    private void onGuardar() {
        if (invoice == null) {
            return;
        }
        try {
            CreatePaymentRequest request = new CreatePaymentRequest();
            request.setFacturaId(invoice.getId());
            if (rbPlazos != null && rbPlazos.isSelected()) {
                request.setTipo(PaymentType.PLAZOS);
                request.setNumeroPlazos(parsePlazos());
                request.setPrimerVencimiento(dpPrimerVencimiento != null ? dpPrimerVencimiento.getValue() : null);
                request.setInteresPorcentaje(parseInteres());
            } else {
                request.setTipo(PaymentType.CONTADO);
                request.setNumeroPlazos(1);
                request.setPrimerVencimiento(LocalDate.now());
                request.setInteresPorcentaje(BigDecimal.ZERO);
            }

            createPaymentUseCase.execute(request);
            saved = true;
            new Alert(Alert.AlertType.INFORMATION, "Pago registrado correctamente.").showAndWait();
            close();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "No se pudo registrar el pago: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onCancelar() {
        close();
    }

    private void updateInstallmentFields() {
        boolean showInstallments = rbPlazos != null && rbPlazos.isSelected();
        if (txtPlazos != null) {
            txtPlazos.setDisable(!showInstallments);
        }
        if (dpPrimerVencimiento != null) {
            dpPrimerVencimiento.setDisable(!showInstallments);
        }
        if (txtInteres != null) {
            txtInteres.setDisable(!showInstallments);
        }
    }

    private void validateForm() {
        boolean valid = invoice != null;
        if (rbPlazos != null && rbPlazos.isSelected()) {
            valid = valid && parsePlazos() > 0 && dpPrimerVencimiento != null && dpPrimerVencimiento.getValue() != null && parseInteres() != null;
        }
        if (btnGuardar != null) {
            btnGuardar.setDisable(!valid);
        }
    }

    private int parsePlazos() {
        if (txtPlazos == null) {
            return 0;
        }
        try {
            return Integer.parseInt(txtPlazos.getText().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private BigDecimal parseInteres() {
        if (txtInteres == null) {
            return BigDecimal.ZERO;
        }
        String raw = txtInteres.getText();
        if (raw == null || raw.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal value = new BigDecimal(raw.trim().replace(',', '.'));
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String formatPrice(BigDecimal value) {
        if (value == null) {
            return "0,00";
        }
        return priceFormat.format(value);
    }

    private void close() {
        if (btnGuardar != null) {
            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            stage.close();
        }
    }
}
