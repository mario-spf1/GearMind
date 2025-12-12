package com.gearmind.presentation.controller;

import com.gearmind.application.common.SessionManager;
import com.gearmind.application.customer.SaveCustomerRequest;
import com.gearmind.application.customer.SaveCustomerUseCase;
import com.gearmind.domain.customer.Customer;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ClienteFormController {

    @FXML
    private Label lblTitulo;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtTelefono;

    @FXML
    private TextArea txtNotas;
    
    @FXML
    private Button btnGuardar;

    private final SaveCustomerUseCase saveCustomerUseCase;

    private Long editingId = null;
    private boolean saved = false;

    public ClienteFormController() {
        this.saveCustomerUseCase = new SaveCustomerUseCase(
                new MySqlCustomerRepository()
        );
    }

    public void initForNew() {
        editingId = null;
        lblTitulo.setText("Nuevo cliente");
    }

    public void initForEdit(Customer customer) {
        editingId = customer.getId();
        lblTitulo.setText("Editar cliente");

        txtNombre.setText(customer.getNombre());
        txtEmail.setText(customer.getEmail());
        txtTelefono.setText(customer.getTelefono());
        txtNotas.setText(customer.getNotas());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onGuardar() {
        try {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();

            String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
            if (nombre.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "El nombre es obligatorio.").showAndWait();
                return;
            }
            
            String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
            if (!email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                new Alert(Alert.AlertType.WARNING, "El email no tiene un formato válido.").showAndWait();
                return;
            }
            
            String telefono = txtTelefono.getText() != null ? txtTelefono.getText().trim().replaceAll("\\s+", " ") : "";
            String notas = txtNotas.getText() != null ? txtNotas.getText().trim() : "";

            SaveCustomerRequest request = new SaveCustomerRequest(editingId, empresaId, nombre, email, telefono, notas);
            Customer result = saveCustomerUseCase.save(request);
            System.out.println("Cliente guardado: " + result.getId() + " - " + result.getNombre());

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
            alert.setHeaderText("No se pudo guardar el cliente");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onCancelar() {
        saved = false;
        closeWindow();
    }
    
    @FXML
    private void initialize() {
        btnGuardar.setDisable(true);
        txtNombre.textProperty().addListener((obs, oldV, newV) -> {
            boolean invalid = (newV == null || newV.trim().isEmpty());
            btnGuardar.setDisable(invalid);
        });
    }

    private void closeWindow() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }
}
