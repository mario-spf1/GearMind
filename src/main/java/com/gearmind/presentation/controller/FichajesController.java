package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.fichaje.GetFichajeStatusUseCase;
import com.gearmind.application.fichaje.RegistrarFichajeUseCase;
import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.fichaje.Fichaje;
import com.gearmind.domain.fichaje.FichajeMovimiento;
import com.gearmind.domain.fichaje.FichajeStatus;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import com.gearmind.infrastructure.company.MySqlEmpresaRepository;
import com.gearmind.infrastructure.fichaje.MySqlFichajeRepository;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class FichajesController {

    @FXML
    private Label lblSubtitulo;
    @FXML
    private Label lblTiempoActual;
    @FXML
    private Button btnSesionLaboral;
    @FXML
    private Label lblEmpresaFiltro;
    @FXML
    private ComboBox<EmpresaOption> cmbEmpresa;
    @FXML
    private Label lblEmpleadoFiltro;
    @FXML
    private ComboBox<UsuarioOption> cmbEmpleado;
    @FXML
    private TableView<Fichaje> tblFichajes;
    @FXML
    private TableColumn<Fichaje, String> colFecha;
    @FXML
    private TableColumn<Fichaje, String> colMovimiento;
    @FXML
    private TableColumn<Fichaje, String> colEmpleado;
    @FXML
    private TableColumn<Fichaje, String> colEmpresa;

    private final MySqlFichajeRepository fichajeRepository;
    private final MySqlEmpresaRepository empresaRepository;
    private final MySqlUserRepository userRepository;
    private final RegistrarFichajeUseCase registrarFichajeUseCase;
    private final GetFichajeStatusUseCase getFichajeStatusUseCase;
    private final ObservableList<Fichaje> fichajes = FXCollections.observableArrayList();
    private final ObservableList<EmpresaOption> empresas = FXCollections.observableArrayList();
    private final ObservableList<UsuarioOption> usuarios = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Timeline timer;

    public FichajesController() {
        this.fichajeRepository = new MySqlFichajeRepository();
        this.empresaRepository = new MySqlEmpresaRepository();
        this.userRepository = new MySqlUserRepository();
        this.registrarFichajeUseCase = new RegistrarFichajeUseCase(fichajeRepository);
        this.getFichajeStatusUseCase = new GetFichajeStatusUseCase(fichajeRepository);
    }

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        refreshFichajes();
        refreshStatus();
        startTimer();
    }

    private void setupTable() {
        if (tblFichajes == null) {
            return;
        }
        colFecha.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getFecha())));
        colMovimiento.setCellValueFactory(cell -> new SimpleStringProperty(formatMovimiento(cell.getValue().getMovimiento())));
        colEmpleado.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getUsuarioNombre())));
        colEmpresa.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getEmpresaNombre())));
        tblFichajes.setItems(fichajes);
        tblFichajes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupFilters() {
        UserRole role = AuthContext.getRole();
        if (role == UserRole.SUPER_ADMIN) {
            lblEmpleadoFiltro.setText("Usuario:");
            loadEmpresas();
            if (cmbEmpresa != null) {
                cmbEmpresa.valueProperty().addListener((obs, oldVal, newVal) -> {
                    loadUsuariosByEmpresa(newVal != null ? newVal.id : 0L);
                    refreshFichajes();
                });
            }
            if (cmbEmpleado != null) {
                cmbEmpleado.valueProperty().addListener((obs, oldVal, newVal) -> refreshFichajes());
            }
            return;
        }

        if (role == UserRole.ADMIN) {
            hideNode(lblEmpresaFiltro);
            hideNode(cmbEmpresa);
            loadUsuariosByEmpresa(AuthContext.getEmpresaId());
            if (cmbEmpleado != null) {
                cmbEmpleado.valueProperty().addListener((obs, oldVal, newVal) -> refreshFichajes());
            }
            return;
        }

        hideNode(lblEmpresaFiltro);
        hideNode(cmbEmpresa);
        hideNode(lblEmpleadoFiltro);
        hideNode(cmbEmpleado);
        hideColumn(colEmpleado);
        hideColumn(colEmpresa);
    }

    private void loadEmpresas() {
        empresas.clear();
        empresas.add(new EmpresaOption(0L, "Todas"));
        List<Empresa> all = empresaRepository.findAll();
        all.forEach(e -> empresas.add(new EmpresaOption(e.getId(), e.getNombre())));
        if (cmbEmpresa != null) {
            cmbEmpresa.setItems(empresas);
            cmbEmpresa.getSelectionModel().selectFirst();
        }
        loadUsuariosByEmpresa(0L);
    }

    private void loadUsuariosByEmpresa(long empresaId) {
        usuarios.clear();
        usuarios.add(new UsuarioOption(0L, "Todos"));
        if (empresaId > 0) {
            List<User> users = userRepository.findByEmpresaId(empresaId);
            users.forEach(u -> usuarios.add(new UsuarioOption(u.getId(), u.getNombre())));
        } else if (AuthContext.isAdmin()) {
            List<User> users = userRepository.findByEmpresaId(AuthContext.getEmpresaId());
            users.forEach(u -> usuarios.add(new UsuarioOption(u.getId(), u.getNombre())));
        }
        if (cmbEmpleado != null) {
            cmbEmpleado.setItems(usuarios);
            cmbEmpleado.getSelectionModel().selectFirst();
        }
    }

    private void refreshFichajes() {
        UserRole role = AuthContext.getRole();
        Long empresaId = null;
        Long userId = null;

        if (role == UserRole.EMPLEADO) {
            empresaId = AuthContext.getEmpresaId();
            userId = AuthContext.getCurrentUser().getId();
        } else if (role == UserRole.ADMIN) {
            empresaId = AuthContext.getEmpresaId();
            userId = cmbEmpleado != null && cmbEmpleado.getValue() != null ? cmbEmpleado.getValue().id : null;
        } else if (role == UserRole.SUPER_ADMIN) {
            empresaId = cmbEmpresa != null && cmbEmpresa.getValue() != null ? cmbEmpresa.getValue().id : null;
            userId = cmbEmpleado != null && cmbEmpleado.getValue() != null ? cmbEmpleado.getValue().id : null;
        }

        fichajes.setAll(fichajeRepository.findByFilters(empresaId, userId));
    }

    @FXML
    private void onSesionLaboral() {
        if (!AuthContext.isLoggedIn()) {
            new Alert(Alert.AlertType.WARNING, "Necesitas iniciar sesión para fichar.").showAndWait();
            return;
        }
        FichajeMovimiento next = resolveNextMovimiento();
        String action = next == FichajeMovimiento.ENTRADA ? "Comenzar sesión laboral" : "Terminar sesión laboral";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Fichajes");
        confirm.setHeaderText(action);
        confirm.setContentText("¿Quieres registrar " + action.toLowerCase() + "?");
        confirm.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                try {
                    registrarFichajeUseCase.registrar(AuthContext.getEmpresaId(), AuthContext.getCurrentUser().getId(), next);
                    refreshStatus();
                    refreshFichajes();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "No se pudo registrar el fichaje: " + ex.getMessage()).showAndWait();
                }
            }
        });
    }

    private void refreshStatus() {
        Optional<FichajeStatus> status = getFichajeStatusUseCase.execute(AuthContext.getCurrentUser().getId());
        if (status.isEmpty() || status.get().movimientoActual() == FichajeMovimiento.SALIDA) {
            btnSesionLaboral.setText("Comenzar sesión laboral");
            lblTiempoActual.setText("Tiempo: 00:00 h");
        } else {
            btnSesionLaboral.setText("Terminar sesión laboral");
            lblTiempoActual.setText("Tiempo: " + formatDuration(status.get().tiempoTrabajado(), status.get().fechaUltimoMovimiento()));
        }
    }

    private void startTimer() {
        if (timer != null) {
            timer.stop();
        }
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshStatus()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private FichajeMovimiento resolveNextMovimiento() {
        Optional<FichajeStatus> status = getFichajeStatusUseCase.execute(AuthContext.getCurrentUser().getId());
        if (status.isEmpty()) {
            return FichajeMovimiento.ENTRADA;
        }
        return status.get().movimientoActual() == FichajeMovimiento.ENTRADA
                ? FichajeMovimiento.SALIDA
                : FichajeMovimiento.ENTRADA;
    }

    private String formatDate(LocalDateTime date) {
        return date != null ? date.format(dateFormatter) : "";
    }

    private String formatMovimiento(FichajeMovimiento movimiento) {
        return movimiento == null ? "" : movimiento.name();
    }

    private String formatDuration(java.time.Duration duration, LocalDateTime from) {
        if (duration == null || from == null) {
            return "00:00 h";
        }
        long totalMinutes = duration.toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%02d:%02d h", hours, minutes);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void hideNode(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        node.setVisible(false);
        node.setManaged(false);
    }

    private void hideColumn(TableColumn<?, ?> column) {
        if (column == null) {
            return;
        }
        column.setVisible(false);
    }

    private record EmpresaOption(long id, String label) {

        @Override
        public String toString() {
            return label;
        }
    }

    private record UsuarioOption(long id, String label) {

        @Override
        public String toString() {
            return label;
        }
    }
}
