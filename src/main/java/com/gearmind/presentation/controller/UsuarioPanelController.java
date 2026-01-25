package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.fichaje.GetFichajeStatusUseCase;
import com.gearmind.application.user.GetUserStatsUseCase;
import com.gearmind.application.user.SaveUserRequest;
import com.gearmind.application.user.SaveUserUseCase;
import com.gearmind.application.user.UserStats;
import com.gearmind.domain.fichaje.FichajeMovimiento;
import com.gearmind.domain.fichaje.FichajeStatus;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.auth.BCryptPasswordHasher;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import com.gearmind.infrastructure.fichaje.MySqlFichajeRepository;
import com.gearmind.infrastructure.repair.MySqlRepairRepository;
import com.gearmind.infrastructure.task.MySqlTaskRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class UsuarioPanelController {

    @FXML
    private Label lblUsuarioNombre;
    @FXML
    private Label lblTareasAsignadas;
    @FXML
    private Label lblReparaciones;
    @FXML
    private Label lblHorasHoy;
    @FXML
    private Label lblUltimoFichaje;
    @FXML
    private Label lblEstadoFichaje;
    @FXML
    private Label lblEmail;
    @FXML
    private Label lblRol;
    @FXML
    private Label lblEmpresa;
    @FXML
    private Label lblHorasDetalle;
    @FXML
    private Label lblTareasDetalle;
    @FXML
    private Label lblReparacionesDetalle;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Button btnGuardarPerfil;
    @FXML
    private Label lblGuardarEstado;

    private final GetUserStatsUseCase statsUseCase;
    private final GetFichajeStatusUseCase fichajeStatusUseCase;
    private final SaveUserUseCase saveUserUseCase;

    public UsuarioPanelController() {
        this.statsUseCase = new GetUserStatsUseCase(new MySqlTaskRepository(), new MySqlRepairRepository());
        this.fichajeStatusUseCase = new GetFichajeStatusUseCase(new MySqlFichajeRepository());
        this.saveUserUseCase = new SaveUserUseCase(new MySqlUserRepository(), new BCryptPasswordHasher());
    }

    @FXML
    public void initialize() {
        loadUserData();
        loadStats();
        loadFichajeStatus();
        setupProfileForm();
    }

    private void loadUserData() {
        if (!AuthContext.isLoggedIn()) {
            return;
        }

        User user = AuthContext.getCurrentUser();
        if (lblUsuarioNombre != null) {
            lblUsuarioNombre.setText(user.getNombre());
        }
        if (lblEmail != null) {
            lblEmail.setText("Email: " + user.getEmail());
        }
        if (txtNombre != null) {
            txtNombre.setText(user.getNombre());
        }
        if (txtEmail != null) {
            txtEmail.setText(user.getEmail());
        }
        if (lblRol != null) {
            UserRole role = AuthContext.getRole();
            String rolTexto = switch (role) {
                case SUPER_ADMIN ->
                    "Super administrador";
                case ADMIN ->
                    "Administrador";
                case EMPLEADO ->
                    "Empleado";
            };
            lblRol.setText("Rol: " + rolTexto);
        }
        if (lblEmpresa != null) {
            String empresaNombre = SessionManager.getInstance().getCurrentEmpresaNombre();
            if (empresaNombre == null || empresaNombre.isBlank()) {
                lblEmpresa.setText("Empresa: —");
            } else {
                lblEmpresa.setText("Empresa: " + empresaNombre);
            }
        }
    }

    private void setupProfileForm() {
        if (btnGuardarPerfil == null) {
            return;
        }
        btnGuardarPerfil.setDisable(true);
        if (txtNombre != null) {
            txtNombre.textProperty().addListener((obs, oldVal, newVal) -> validateProfileForm());
        }
        if (txtEmail != null) {
            txtEmail.textProperty().addListener((obs, oldVal, newVal) -> validateProfileForm());
        }
        validateProfileForm();
    }

    private void validateProfileForm() {
        if (btnGuardarPerfil == null) {
            return;
        }
        String nombre = txtNombre != null ? txtNombre.getText().trim() : "";
        String email = txtEmail != null ? txtEmail.getText().trim() : "";
        btnGuardarPerfil.setDisable(nombre.isBlank() || email.isBlank());
    }

    private void loadStats() {
        if (!AuthContext.isLoggedIn()) {
            return;
        }

        UserStats stats = statsUseCase.execute(AuthContext.getCurrentUser().getId());
        if (lblTareasAsignadas != null) {
            lblTareasAsignadas.setText(String.valueOf(stats.tareasAsignadas()));
        }
        if (lblReparaciones != null) {
            lblReparaciones.setText(String.valueOf(stats.reparacionesEmpresa()));
        }
        if (lblTareasDetalle != null) {
            lblTareasDetalle.setText("• " + stats.tareasAsignadas() + " tareas pendientes");
        }
        if (lblReparacionesDetalle != null) {
            lblReparacionesDetalle.setText("• " + stats.reparacionesEmpresa() + " reparaciones activas");
        }
    }

    private void loadFichajeStatus() {
        if (!AuthContext.isLoggedIn()) {
            return;
        }

        Optional<FichajeStatus> status = fichajeStatusUseCase.execute(AuthContext.getCurrentUser().getId());
        if (status.isEmpty()) {
            if (lblHorasHoy != null) {
                lblHorasHoy.setText("—");
            }
            if (lblUltimoFichaje != null) {
                lblUltimoFichaje.setText("—");
            }
            if (lblEstadoFichaje != null) {
                lblEstadoFichaje.setText("Estado: —");
            }
            if (lblHorasDetalle != null) {
                lblHorasDetalle.setText("• 00:00 h trabajadas");
            }
            return;
        }

        FichajeStatus current = status.get();
        String horasHoy = formatDuration(current.tiempoTrabajado());
        if (lblHorasHoy != null) {
            lblHorasHoy.setText(horasHoy);
        }
        if (lblUltimoFichaje != null) {
            lblUltimoFichaje.setText(formatTime(current.fechaUltimoMovimiento()));
        }
        if (lblEstadoFichaje != null) {
            lblEstadoFichaje.setText("Estado: " + formatMovimiento(current.movimientoActual()));
        }
        if (lblHorasDetalle != null) {
            lblHorasDetalle.setText("• " + horasHoy + " trabajadas");
        }
    }

    @FXML
    private void onGuardarPerfil() {
        if (!AuthContext.isLoggedIn()) {
            return;
        }
        try {
            User current = AuthContext.getCurrentUser();
            String nombre = txtNombre != null ? txtNombre.getText().trim() : "";
            String email = txtEmail != null ? txtEmail.getText().trim() : "";
            String password = txtPassword != null ? txtPassword.getText() : null;
            SaveUserRequest request = new SaveUserRequest(current.getId(), current.getEmpresaId(), nombre, email, password, current.getRol(), current.isActivo());
            User updated = saveUserUseCase.save(request);
            SessionManager.getInstance().startSession(updated, SessionManager.getInstance().getCurrentEmpresaNombre());
            if (txtPassword != null) {
                txtPassword.clear();
            }
            loadUserData();
            setProfileStatus("Cambios guardados.", true);
        } catch (Exception ex) {
            ex.printStackTrace();
            setProfileStatus("No se pudo guardar: " + ex.getMessage(), false);
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar el perfil: " + ex.getMessage()).showAndWait();
        }
    }

    private void setProfileStatus(String message, boolean success) {
        if (lblGuardarEstado == null) {
            return;
        }
        lblGuardarEstado.setText(message);
        lblGuardarEstado.getStyleClass().removeAll("tfx-ok", "tfx-error");
        lblGuardarEstado.getStyleClass().add(success ? "tfx-ok" : "tfx-error");
    }

    private String formatDuration(Duration duration) {
        if (duration == null) {
            return "00:00 h";
        }
        long totalMinutes = duration.toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%02d:%02d h", hours, minutes);
    }

    private String
            formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "—";
        }
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(formatter);
    }

    private String formatMovimiento(FichajeMovimiento movimiento) {
        if (movimiento == null) {
            return "—";
        }
        return movimiento == FichajeMovimiento.ENTRADA ? "En sesión" : "Fuera";
    }
}
