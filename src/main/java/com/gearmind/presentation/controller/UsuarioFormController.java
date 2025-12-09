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

import java.util.Arrays;

public class UsuarioFormController {

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField pwdPassword;

    @FXML
    private ComboBox<UserRole> cmbRol;

    @FXML
    private CheckBox chkActivo;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnCancelar;

    private final SaveUserUseCase saveUserUseCase;

    private User editingUser;
    private boolean saved = false;

    public UsuarioFormController() {
        var repo = new MySqlUserRepository();
        PasswordHasher hasher = new BCryptPasswordHasher();
        this.saveUserUseCase = new SaveUserUseCase(repo, hasher);
    }

    @FXML
    private void initialize() {
        // Rellenar combo de roles
        cmbRol.getItems().setAll(Arrays.asList(UserRole.values()));

        // Por defecto activo
        chkActivo.setSelected(true);
    }

    /**
     * Se llama desde el UsuariosController para pasar el usuario a editar.
     * Si es null, el formulario se usa para crear uno nuevo.
     */
    public void setUser(User user) {
        this.editingUser = user;

        if (user == null) {
            // Nuevo usuario
            txtNombre.clear();
            txtEmail.clear();
            pwdPassword.clear();
            cmbRol.getSelectionModel().clearSelection();
            chkActivo.setSelected(true);
        } else {
            // Edición
            txtNombre.setText(user.getNombre());
            txtEmail.setText(user.getEmail());
            cmbRol.getSelectionModel().select(user.getRol());
            chkActivo.setSelected(user.isActivo());
            pwdPassword.clear(); // nunca mostramos la contraseña
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onGuardar() {
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        String rawPassword = pwdPassword.getText() != null ? pwdPassword.getText().trim() : "";
        UserRole rol = cmbRol.getSelectionModel().getSelectedItem();
        boolean activo = chkActivo.isSelected();

        if (nombre.isBlank()) {
            showError("El nombre es obligatorio.");
            return;
        }
        if (email.isBlank()) {
            showError("El email es obligatorio.");
            return;
        }
        if (rol == null) {
            showError("Debes seleccionar un rol.");
            return;
        }

        long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
        Long userId = editingUser != null ? editingUser.getId() : null;

        // NOTA: podemos dejar la contraseña en blanco en edición para no cambiarla;
        // el SaveUserUseCase debería ignorar password en blanco.
        SaveUserRequest request = new SaveUserRequest(
                userId,
                empresaId,
                nombre,
                email,
                rawPassword,
                rol,
                activo
        );

        try {
            saveUserUseCase.save(request);
            this.saved = true;
            closeWindow();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("No se ha podido guardar el usuario: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        this.saved = false;
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Validación");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
