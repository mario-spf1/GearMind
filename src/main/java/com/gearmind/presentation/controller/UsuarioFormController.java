package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.user.SaveUserRequest;
import com.gearmind.application.user.SaveUserUseCase;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.auth.BCryptPasswordHasher;
import com.gearmind.infrastructure.database.DataSourceFactory;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.Node;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

public class UsuarioFormController {

    @FXML private Label lblTitulo;

    // Empresa (solo SuperAdmin)
    @FXML private Label lblEmpresa;
    @FXML private HBox boxEmpresa;
    @FXML private ComboBox<EmpresaOption> cmbEmpresa;

    @FXML private TextField txtNombre;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<UserRole> cmbRol;
    @FXML private CheckBox chkActivo;

    @FXML private Button btnGuardar;

    private final SaveUserUseCase saveUserUseCase;

    private final DataSource dataSource = DataSourceFactory.getDataSource();
    private final ObservableList<EmpresaOption> empresas = FXCollections.observableArrayList();
    private FilteredList<EmpresaOption> empresasFiltradas;

    private Long editingId = null;
    private long selectedEmpresaId = 0;
    private boolean saved = false;

    public UsuarioFormController() {
        this.saveUserUseCase = new SaveUserUseCase(
                new MySqlUserRepository(),
                new BCryptPasswordHasher()
        );
    }

    public boolean isSaved() {
        return saved;
    }

    public void initForNew() {
        editingId = null;
        if (lblTitulo != null) lblTitulo.setText("Nuevo usuario");

        txtNombre.clear();
        txtEmail.clear();
        txtPassword.clear();

        if (cmbRol != null) cmbRol.getSelectionModel().select(UserRole.EMPLEADO);
        if (chkActivo != null) chkActivo.setSelected(true);

        if (AuthContext.isSuperAdmin()) {
            selectedEmpresaId = 0;
            if (cmbEmpresa != null) cmbEmpresa.getSelectionModel().clearSelection();
        } else {
            selectedEmpresaId = SessionManager.getInstance().getCurrentEmpresaId();
        }
    }

    public void initForEdit(User user) {
        editingId = user.getId();
        if (lblTitulo != null) lblTitulo.setText("Editar usuario");

        txtNombre.setText(nullToEmpty(user.getNombre()));
        txtEmail.setText(nullToEmpty(user.getEmail()));
        txtPassword.clear(); // opcional: si está vacío, mantiene la existente

        if (cmbRol != null) cmbRol.getSelectionModel().select(user.getRol());
        if (chkActivo != null) chkActivo.setSelected(user.isActivo());

        if (AuthContext.isSuperAdmin()) {
            selectedEmpresaId = user.getEmpresaId();

            // seleccionar empresa (cuando ya estén cargadas)
            if (cmbEmpresa != null && !empresas.isEmpty()) {
                EmpresaOption opt = empresas.stream()
                        .filter(e -> e.id == selectedEmpresaId)
                        .findFirst().orElse(null);
                if (opt != null) cmbEmpresa.getSelectionModel().select(opt);
            }
        } else {
            selectedEmpresaId = SessionManager.getInstance().getCurrentEmpresaId();
        }
    }

    @FXML
    private void initialize() {
        // Guard: empleados no deberían entrar aquí igualmente
        if (AuthContext.isEmpleado()) {
            bloquearTodo("Acceso restringido.");
            return;
        }

        if (cmbRol != null) {
            cmbRol.setItems(FXCollections.observableArrayList(UserRole.values()));
            cmbRol.getSelectionModel().select(UserRole.EMPLEADO);
        }

        if (chkActivo != null) chkActivo.setSelected(true);

        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        if (!isSuperAdmin) {
            // ocultar empresa
            hideNode(lblEmpresa);
            hideNode(boxEmpresa);
            selectedEmpresaId = SessionManager.getInstance().getCurrentEmpresaId();
        } else {
            showNode(lblEmpresa);
            showNode(boxEmpresa);

            loadEmpresas();
            enableComboSearchEmpresa(cmbEmpresa);

            if (cmbEmpresa != null) {
                cmbEmpresa.valueProperty().addListener((obs, o, n) -> {
                    if (n != null) selectedEmpresaId = n.id;
                });
            }
        }
    }

    @FXML
    private void onGuardar() {
        try {
            if (AuthContext.isEmpleado()) return;

            long empresaId;
            if (AuthContext.isSuperAdmin()) {
                if (cmbEmpresa == null || cmbEmpresa.getValue() == null) {
                    new Alert(Alert.AlertType.WARNING, "Selecciona una empresa.").showAndWait();
                    return;
                }
                empresaId = selectedEmpresaId;
            } else {
                empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            }

            String nombre = safeTrim(txtNombre.getText());
            String email = safeTrim(txtEmail.getText());
            String rawPassword = (txtPassword != null) ? txtPassword.getText() : null;

            UserRole rol = (cmbRol != null && cmbRol.getValue() != null) ? cmbRol.getValue() : UserRole.EMPLEADO;
            boolean activo = chkActivo != null && chkActivo.isSelected();

            SaveUserRequest req = new SaveUserRequest(
                    editingId,
                    empresaId,
                    nombre,
                    email,
                    rawPassword,
                    rol,
                    activo
            );

            saveUserUseCase.save(req);

            saved = true;
            closeWindow();

        } catch (IllegalArgumentException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar el usuario: " + ex.getMessage()).showAndWait();
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

    // ===== Empresas =====

    private void loadEmpresas() {
        empresas.clear();
        String sql = "SELECT id, nombre FROM empresa ORDER BY nombre ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                empresas.add(new EmpresaOption(rs.getLong("id"), rs.getString("nombre")));
            }
        } catch (Exception ex) {
            throw new RuntimeException("No se pudieron cargar las empresas", ex);
        }
    }

    /**
     * Buscador estable (no castea String->EmpresaOption, evita el bug que comentabas).
     */
    private void enableComboSearchEmpresa(ComboBox<EmpresaOption> combo) {
        if (combo == null) return;

        combo.setEditable(true);

        combo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(EmpresaOption opt) {
                return opt == null ? "" : opt.nombre;
            }
            @Override
            public EmpresaOption fromString(String s) {
                // NO devolvemos un String, siempre EmpresaOption o null
                if (s == null) return null;
                String t = s.trim();
                if (t.isEmpty()) return null;

                return empresas.stream()
                        .filter(e -> e.nombre != null && e.nombre.equalsIgnoreCase(t))
                        .findFirst().orElse(null);
            }
        });

        empresasFiltradas = new FilteredList<>(empresas, e -> true);
        combo.setItems(empresasFiltradas);

        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(EmpresaOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.nombre);
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(EmpresaOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.nombre);
            }
        });

        combo.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            String f = (newText == null ? "" : newText).toLowerCase(Locale.ROOT).trim();

            empresasFiltradas.setPredicate(opt ->
                    f.isEmpty() || (opt.nombre != null && opt.nombre.toLowerCase(Locale.ROOT).contains(f))
            );

            if (!combo.isShowing()) combo.show();
        });

        combo.getEditor().focusedProperty().addListener((obs, was, is) -> {
            if (is) return;
            EmpresaOption match = combo.getConverter().fromString(combo.getEditor().getText());
            if (match != null) combo.getSelectionModel().select(match);
            else combo.getSelectionModel().clearSelection();
        });
    }

    private static class EmpresaOption {
        final long id;
        final String nombre;

        EmpresaOption(long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override public String toString() {
            return nombre;
        }
    }

    // ===== Helpers =====

    private void bloquearTodo(String msg) {
        if (btnGuardar != null) btnGuardar.setDisable(true);
        if (txtNombre != null) txtNombre.setDisable(true);
        if (txtEmail != null) txtEmail.setDisable(true);
        if (txtPassword != null) txtPassword.setDisable(true);
        if (cmbRol != null) cmbRol.setDisable(true);
        if (chkActivo != null) chkActivo.setDisable(true);
        if (lblTitulo != null) lblTitulo.setText(msg);
    }

    private void hideNode(Node n) {
        if (n == null) return;
        n.setVisible(false);
        n.setManaged(false);
    }

    private void showNode(Node n) {
        if (n == null) return;
        n.setVisible(true);
        n.setManaged(true);
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
