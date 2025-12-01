package com.gearmind.presentation.controller;

import com.gearmind.application.common.SessionManager;
import com.gearmind.application.customer.SaveCustomerRequest;
import com.gearmind.application.customer.SaveCustomerUseCase;
import com.gearmind.domain.customer.Customer;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

    private final SaveCustomerUseCase saveCustomerUseCase;

    private Long editingId = null; // null -> nuevo, no null -> editar
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
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onGuardar() {
        try {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();

            SaveCustomerRequest request = new SaveCustomerRequest(
                    editingId,
                    empresaId,
                    txtNombre.getText(),
                    txtEmail.getText(),
                    txtTelefono.getText()
            );

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

    private void closeWindow() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }
}
