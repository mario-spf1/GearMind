package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.company.SaveEmpresaRequest;
import com.gearmind.application.company.SaveEmpresaUseCase;
import com.gearmind.domain.company.Empresa;
import com.gearmind.infrastructure.company.MySqlEmpresaRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class EmpresaFormController {

    @FXML
    private Label lblTitulo;
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

    private final SaveEmpresaUseCase saveEmpresaUseCase;

    private Long editingId = null;   // null => nueva
    private boolean saved = false;

    public EmpresaFormController() {
        this.saveEmpresaUseCase = new SaveEmpresaUseCase(new MySqlEmpresaRepository());
    }

    public boolean isSaved() {
        return saved;
    }

    /**
     * Igual que los otros: prepara el formulario para "Nueva empresa"
     */
    public void initForNew() {
        editingId = null;
        saved = false;

        if (lblTitulo != null) {
            lblTitulo.setText("Nueva empresa");
        }
        limpiarFormulario();

        if (chkActiva != null) {
            chkActiva.setSelected(true);
        }
        ocultarError();
    }

    /**
     * Igual que los otros: prepara el formulario para "Editar empresa"
     */
    public void initForEdit(Empresa empresa) {
        if (empresa == null) {
            initForNew();
            return;
        }

        editingId = empresa.getId();
        saved = false;

        if (lblTitulo != null) {
            lblTitulo.setText("Editar empresa");
        }

        txtNombre.setText(nullToEmpty(empresa.getNombre()));
        txtCif.setText(nullToEmpty(empresa.getCif()));
        txtTelefono.setText(nullToEmpty(empresa.getTelefono()));
        txtEmail.setText(nullToEmpty(empresa.getEmail()));
        txtDireccion.setText(nullToEmpty(empresa.getDireccion()));
        txtCiudad.setText(nullToEmpty(empresa.getCiudad()));
        txtProvincia.setText(nullToEmpty(empresa.getProvincia()));
        txtCp.setText(nullToEmpty(empresa.getCp()));
        if (chkActiva != null) {
            chkActiva.setSelected(empresa.isActiva());
        }

        ocultarError();
        refreshGuardarEnabled();
    }

    @FXML
    private void initialize() {
        ocultarError();

        // ✅ Patrón igual: guardar solo activo si hay nombre
        if (btnGuardar != null) {
            btnGuardar.setDisable(true);
        }

        if (txtNombre != null) {
            txtNombre.textProperty().addListener((obs, oldV, newV) -> refreshGuardarEnabled());
        }

        // ✅ Guard por rol (igual que estabas haciendo)
        if (!AuthContext.isSuperAdmin()) {
            bloquearFormulario();
            mostrarError("Acceso restringido: solo SuperAdmin puede gestionar empresas.");
        } else {
            // Estado inicial por defecto si no llaman initForNew/initForEdit
            if (lblTitulo != null) {
                lblTitulo.setText("Empresa");
            }
            if (chkActiva != null) {
                chkActiva.setSelected(true);
            }
        }
    }

    private void refreshGuardarEnabled() {
        if (btnGuardar == null) {
            return;
        }

        String nombre = txtNombre != null ? safeTrim(txtNombre.getText()) : "";
        btnGuardar.setDisable(nombre.isBlank());
    }

    @FXML
    private void onGuardar() {
        if (!AuthContext.isSuperAdmin()) {
            return;
        }

        ocultarError();

        try {
            String nombre = safeTrim(txtNombre.getText());
            if (nombre.isBlank()) {
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
            boolean activa = chkActiva != null && chkActiva.isSelected();

            SaveEmpresaRequest request = new SaveEmpresaRequest(
                    editingId,
                    nombre,
                    cif,
                    telefono,
                    email,
                    direccion,
                    ciudad,
                    provincia,
                    cp,
                    activa
            );

            saveEmpresaUseCase.execute(request);

            saved = true;
            closeWindow();

        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            mostrarError("Se ha producido un error al guardar la empresa.");
        }
    }

    @FXML
    private void onCancelar() {
        saved = false;
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }

    private void limpiarFormulario() {
        if (txtNombre != null) {
            txtNombre.clear();
        }
        if (txtCif != null) {
            txtCif.clear();
        }
        if (txtTelefono != null) {
            txtTelefono.clear();
        }
        if (txtEmail != null) {
            txtEmail.clear();
        }
        if (txtDireccion != null) {
            txtDireccion.clear();
        }
        if (txtCiudad != null) {
            txtCiudad.clear();
        }
        if (txtProvincia != null) {
            txtProvincia.clear();
        }
        if (txtCp != null) {
            txtCp.clear();
        }

        if (chkActiva != null) {
            chkActiva.setSelected(true);
        }
        ocultarError();
        refreshGuardarEnabled();
    }

    private void bloquearFormulario() {
        if (txtNombre != null) {
            txtNombre.setDisable(true);
        }
        if (txtCif != null) {
            txtCif.setDisable(true);
        }
        if (txtTelefono != null) {
            txtTelefono.setDisable(true);
        }
        if (txtEmail != null) {
            txtEmail.setDisable(true);
        }
        if (txtDireccion != null) {
            txtDireccion.setDisable(true);
        }
        if (txtCiudad != null) {
            txtCiudad.setDisable(true);
        }
        if (txtProvincia != null) {
            txtProvincia.setDisable(true);
        }
        if (txtCp != null) {
            txtCp.setDisable(true);
        }
        if (chkActiva != null) {
            chkActiva.setDisable(true);
        }
        if (btnGuardar != null) {
            btnGuardar.setDisable(true);
        }
    }

    private void mostrarError(String mensaje) {
        if (lblError == null) {
            return;
        }
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void ocultarError() {
        if (lblError == null) {
            return;
        }
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
}
