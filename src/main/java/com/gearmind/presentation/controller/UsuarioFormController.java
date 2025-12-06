package com.gearmind.presentation.controller;

import com.gearmind.application.common.SessionManager;
import com.gearmind.application.user.SaveUserRequest;
import com.gearmind.application.user.SaveUserUseCase;
import com.gearmind.domain.security.PasswordHasher;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.auth.BCryptPasswordHasher;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class UsuarioFormController {

    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<UserRole> cmbRol;
    @FXML private CheckBox chkActivo;

    private final SaveUserUseCase saveUserUseCase;

    private Long editingId = null;
    private boolean saved = false;

    public UsuarioFormController() {
        PasswordHasher hasher = new BCryptPasswordHasher();
        this.saveUserUseCase = new SaveUserUseCase(
                new MySqlUserRepository(),
                hasher
        );
    }

    public void initForNew() {
        editingId = null;
        lblTitulo.setText("Nuevo usuario");
        txtNombre.clear();
        txtEmail.clear();
        txtPassword.clear();
        cmbRol.getItems().setAll(UserRole.values());
        cmbRol.getSelectionModel().select(UserRole.EMPLEADO);
        chkActivo.setSelected(true);
    }

    public void initForEdit(User user) {
        editingId = user.getId();
        lblTitulo.setText("Editar usuario");
        txtNombre.setText(user.getNombre());
        txtEmail.setText(user.getEmail());
        txtPassword.clear();
        cmbRol.getItems().setAll(UserRole.values());
        cmbRol.getSelectionModel().select(user.getRol());
        chkActivo.setSelected(user.isActivo());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onGuardar() {
        try {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();

            SaveUserRequest request = new SaveUserRequest(
                    editingId,
                    empresaId,
                    txtNombre.getText(),
                    txtEmail.getText(),
                    txtPassword.getText(),
                    cmbRol.getValue(),
                    chkActivo.isSelected()
            );

            User result = saveUserUseCase.save(request);
            System.out.println("Usuario guardado: " + result.getId() + " - " + result.getEmail());

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
            alert.setHeaderText("No se pudo guardar el usuario");
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
