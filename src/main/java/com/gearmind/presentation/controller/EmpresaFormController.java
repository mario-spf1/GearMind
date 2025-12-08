package com.gearmind.presentation.controller;

import com.gearmind.application.company.SaveEmpresaRequest;
import com.gearmind.application.company.SaveEmpresaUseCase;
import com.gearmind.domain.company.Empresa;
import com.gearmind.infrastructure.company.MySqlEmpresaRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class EmpresaFormController {

    @FXML
    private Label lblTitle;

    @FXML
    private Label lblError;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtCif;

    @FXML
    private TextField txtTelefono;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtDireccion;

    @FXML
    private TextField txtCiudad;

    @FXML
    private TextField txtProvincia;

    @FXML
    private TextField txtCp;

    @FXML
    private CheckBox chkActiva;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnCancelar;

    private final SaveEmpresaUseCase saveEmpresaUseCase;

    /**
     * Empresa que se está editando; null si es nueva.
     */
    private Empresa empresa;

    public EmpresaFormController() {
        this.saveEmpresaUseCase = new SaveEmpresaUseCase(new MySqlEmpresaRepository());
    }

    @FXML
    private void initialize() {
        lblError.setVisible(false);
        lblError.setManaged(false);

        if (chkActiva != null) {
            chkActiva.setSelected(true);
        }

        if (lblTitle != null) {
            lblTitle.setText("Nueva empresa");
        }
    }

    /**
     * Carga una empresa en el formulario para editarla. Si se pasa null, se
     * deja en modo "nueva".
     */
    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;

        if (empresa == null) {
            if (lblTitle != null) {
                lblTitle.setText("Nueva empresa");
            }
            limpiarFormulario();
        } else {
            if (lblTitle != null) {
                lblTitle.setText("Editar empresa");
            }

            txtNombre.setText(nullToEmpty(empresa.getNombre()));
            txtCif.setText(nullToEmpty(empresa.getCif()));
            txtTelefono.setText(nullToEmpty(empresa.getTelefono()));
            txtEmail.setText(nullToEmpty(empresa.getEmail()));
            txtDireccion.setText(nullToEmpty(empresa.getDireccion()));
            txtCiudad.setText(nullToEmpty(empresa.getCiudad()));
            txtProvincia.setText(nullToEmpty(empresa.getProvincia()));
            txtCp.setText(nullToEmpty(empresa.getCp()));
            chkActiva.setSelected(empresa.isActiva());
        }
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtCif.clear();
        txtTelefono.clear();
        txtEmail.clear();
        txtDireccion.clear();
        txtCiudad.clear();
        txtProvincia.clear();
        txtCp.clear();
        chkActiva.setSelected(true);
        ocultarError();
    }

    @FXML
    private void onGuardar() {
        ocultarError();

        try {
            String nombre = safeTrim(txtNombre.getText());
            if (nombre.isEmpty()) {
                mostrarError("El nombre de la empresa es obligatorio.");
                return;
            }

            String cif = safeTrim(txtCif.getText());
            String telefono = safeTrim(txtTelefono.getText());
            String email = safeTrim(txtEmail.getText());
            String direccion = safeTrim(txtDireccion.getText());
            String ciudad = safeTrim(txtCiudad.getText());
            String provincia = safeTrim(txtProvincia.getText());
            String cp = safeTrim(txtCp.getText());
            boolean activa = chkActiva.isSelected();

            Long id = (empresa != null) ? empresa.getId() : null;
            SaveEmpresaRequest request = new SaveEmpresaRequest(id, nombre, cif, telefono, email, direccion, ciudad, provincia, cp, activa);
            saveEmpresaUseCase.execute(request);
            cerrarVentana();

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            mostrarError("Se ha producido un error al guardar la empresa.");
        }
    }

    @FXML
    private void onCancelar() {
        cerrarVentana();
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void ocultarError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}
