package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.fichaje.GetFichajeStatusUseCase;
import com.gearmind.application.user.GetUserStatsUseCase;
import com.gearmind.application.user.UserStats;
import com.gearmind.domain.fichaje.FichajeMovimiento;
import com.gearmind.domain.fichaje.FichajeStatus;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.fichaje.MySqlFichajeRepository;
import com.gearmind.infrastructure.repair.MySqlRepairRepository;
import com.gearmind.infrastructure.task.MySqlTaskRepository;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class UsuarioPanelController {

    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblTareasAsignadas;
    @FXML private Label lblReparaciones;
    @FXML private Label lblTiempoTrabajado;
    @FXML private Label lblEmail;
    @FXML private Label lblRol;
    @FXML private Label lblEmpresa;
    @FXML private Button btnEditarUsuario;

    private final GetUserStatsUseCase statsUseCase;
    private final GetFichajeStatusUseCase fichajeStatusUseCase;

    public UsuarioPanelController() {
        this.statsUseCase = new GetUserStatsUseCase(new MySqlTaskRepository(), new MySqlRepairRepository());
        this.fichajeStatusUseCase = new GetFichajeStatusUseCase(new MySqlFichajeRepository());
    }

    @FXML
    public void initialize() {
        loadUserData();
        loadStats();
        loadFichajeStatus();
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
        if (lblRol != null) {
            UserRole role = AuthContext.getRole();
            String rolTexto = switch (role) {
                case SUPER_ADMIN -> "Super administrador";
                case ADMIN -> "Administrador";
                case EMPLEADO -> "Empleado";
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
    }

    private void loadFichajeStatus() {
        if (!AuthContext.isLoggedIn()) {
            return;
        }

        Optional<FichajeStatus> status = fichajeStatusUseCase.execute(AuthContext.getCurrentUser().getId());
        if (lblTiempoTrabajado != null) {
            if (status.isEmpty() || status.get().movimientoActual() == FichajeMovimiento.SALIDA) {
                lblTiempoTrabajado.setText("—");
            } else {
                lblTiempoTrabajado.setText(formatDuration(status.get().tiempoTrabajado(), status.get().fechaUltimoMovimiento()));
            }
        }
    }

    @FXML
    private void onEditarUsuario() {
        try {
            User current = AuthContext.getCurrentUser();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/UsuarioFormView.fxml"));
            Parent rootForm = loader.load();
            UsuarioFormController controller = loader.getController();
            controller.initForSelfEdit(current);
            Stage stage = new Stage();
            stage.setTitle("Mi cuenta");
            stage.initOwner(btnEditarUsuario.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.setResizable(false);
            Scene scene = new Scene(rootForm);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            if (controller.isSaved()) {
                var repo = new com.gearmind.infrastructure.auth.MySqlUserRepository();
                repo.findById(current.getId()).ifPresent(updated -> {
                    String empresaNombre = SessionManager.getInstance().getCurrentEmpresaNombre();
                    SessionManager.getInstance().startSession(updated, empresaNombre);
                    loadUserData();
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir la edición de usuario: " + ex.getMessage()).showAndWait();
        }
    }

    private String formatDuration(Duration duration, LocalDateTime from) {
        if (duration == null || from == null) {
            return "—";
        }
        long totalMinutes = duration.toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%02d:%02d h", hours, minutes);
    }
}