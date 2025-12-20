package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.user.SaveUserRequest;
import com.gearmind.application.user.SaveUserUseCase;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.database.DataSourceFactory;
import com.gearmind.infrastructure.auth.MySqlUserRepository;
import com.gearmind.infrastructure.auth.BCryptPasswordHasher;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

public class UsuarioFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblEmpresa;
    @FXML
    private HBox boxEmpresa;
    @FXML
    private ComboBox<EmpresaOption> cmbEmpresa;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private ComboBox<UserRole> cmbRol;
    @FXML
    private CheckBox chkActivo;
    @FXML
    private Button btnGuardar;

    private final SaveUserUseCase saveUserUseCase;
    private final DataSource dataSource = DataSourceFactory.getDataSource();
    private final ObservableList<EmpresaOption> empresas = FXCollections.observableArrayList();
    private FilteredList<EmpresaOption> empresasFiltradas;
    private Long editingId = null;
    private boolean saved = false;
    private long selectedEmpresaId = 0;
    private Long pendingEmpresaIdToSelect = null;
    private boolean settingComboProgrammatically = false;
    private boolean selfEditMode = false;
    private UserRole lockedRole = null;
    private boolean lockedActivo = true;
    private long lockedEmpresaId = 0;

    public UsuarioFormController() {
        this.saveUserUseCase = new SaveUserUseCase(new MySqlUserRepository(), new BCryptPasswordHasher());
    }

    public boolean isSaved() {
        return saved;
    }

    public void initForSelfEdit(User user) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser null");
        }

        selfEditMode = true;
        editingId = user.getId();
        saved = false;
        lockedRole = user.getRol();
        lockedActivo = user.isActivo();
        lockedEmpresaId = user.getEmpresaId();

        if (lblTitulo != null) {
            lblTitulo.setText("Mi cuenta");
        }

        if (txtNombre != null) {
            txtNombre.setText(nullToEmpty(user.getNombre()));
        }
        if (txtEmail != null) {
            txtEmail.setText(nullToEmpty(user.getEmail()));
        }
        if (txtPassword != null) {
            txtPassword.clear();
        }
        hideNode(lblEmpresa);
        hideNode(boxEmpresa);
        if (cmbRol != null) {
            cmbRol.setItems(FXCollections.observableArrayList(UserRole.values()));
            cmbRol.getSelectionModel().select(user.getRol());
            cmbRol.setDisable(true);
            cmbRol.setManaged(false);
            cmbRol.setVisible(false);
        }
        if (chkActivo != null) {
            chkActivo.setSelected(user.isActivo());
            chkActivo.setDisable(true);
            chkActivo.setManaged(false);
            chkActivo.setVisible(false);
        }

        validateForm();
    }

    public void initForNew() {
        editingId = null;
        saved = false;

        if (lblTitulo != null) {
            lblTitulo.setText("Nuevo usuario");
        }
        if (txtNombre != null) {
            txtNombre.clear();
        }
        if (txtEmail != null) {
            txtEmail.clear();
        }
        if (txtPassword != null) {
            txtPassword.clear();
        }
        if (cmbRol != null) {
            cmbRol.getSelectionModel().select(UserRole.EMPLEADO);
        }
        if (chkActivo != null) {
            chkActivo.setSelected(true);
        }
        if (AuthContext.isSuperAdmin()) {
            pendingEmpresaIdToSelect = null;
            selectedEmpresaId = 0;
            if (cmbEmpresa != null) {
                cmbEmpresa.getSelectionModel().clearSelection();
            }
        } else {
            selectedEmpresaId = SessionManager.getInstance().getCurrentEmpresaId();
        }
    }

    public void initForEdit(User user) {
        editingId = user.getId();
        saved = false;

        if (lblTitulo != null) {
            lblTitulo.setText("Editar usuario");
        }
        if (txtNombre != null) {
            txtNombre.setText(nullToEmpty(user.getNombre()));
        }
        if (txtEmail != null) {
            txtEmail.setText(nullToEmpty(user.getEmail()));
        }
        if (txtPassword != null) {
            txtPassword.clear();
        }
        if (cmbRol != null) {
            cmbRol.getSelectionModel().select(user.getRol());
        }
        if (chkActivo != null) {
            chkActivo.setSelected(user.isActivo());
        }
        if (AuthContext.isSuperAdmin()) {
            pendingEmpresaIdToSelect = user.getEmpresaId();
        } else {
            selectedEmpresaId = SessionManager.getInstance().getCurrentEmpresaId();
        }
    }

    @FXML
    private void initialize() {

        if (btnGuardar != null) {
            btnGuardar.setDisable(true);
        }

        if (txtNombre != null) {
            txtNombre.textProperty().addListener((obs, o, n) -> validateForm());
        }
        if (txtEmail != null) {
            txtEmail.textProperty().addListener((obs, o, n) -> validateForm());
        }

        if (cmbRol != null) {
            cmbRol.setItems(FXCollections.observableArrayList(UserRole.values()));
            cmbRol.getSelectionModel().select(UserRole.EMPLEADO);
        }

        if (chkActivo != null) {
            chkActivo.setSelected(true);
        }

        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        if (!isSuperAdmin) {
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
                    if (settingComboProgrammatically) {
                        return;
                    }
                    if (n != null) {
                        selectedEmpresaId = n.id;
                    }
                });
            }
            applyPendingEmpresaSelectionIfAny();
        }
        validateForm();
    }

    private void validateForm() {
        if (btnGuardar == null) {
            return;
        }
        String nombre = safeTrim(txtNombre != null ? txtNombre.getText() : "");
        String email = safeTrim(txtEmail != null ? txtEmail.getText() : "");
        boolean ok = !nombre.isBlank() && !email.isBlank();
        btnGuardar.setDisable(!ok);
    }

    @FXML
    private void onGuardar() {
        try {
            long empresaId;

            if (selfEditMode) {
                empresaId = lockedEmpresaId;
            } else if (AuthContext.isSuperAdmin()) {
                if (cmbEmpresa == null || cmbEmpresa.getValue() == null) {
                    new Alert(Alert.AlertType.WARNING, "Selecciona una empresa.").showAndWait();
                    return;
                }
                empresaId = selectedEmpresaId;
            } else {
                empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            }

            String nombre = safeTrim(txtNombre != null ? txtNombre.getText() : "");
            if (nombre.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "El nombre es obligatorio.").showAndWait();
                return;
            }

            String email = safeTrim(txtEmail != null ? txtEmail.getText() : "");
            if (email.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "El email es obligatorio.").showAndWait();
                return;
            }
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                new Alert(Alert.AlertType.WARNING, "El email no tiene un formato válido.").showAndWait();
                return;
            }

            String password = (txtPassword != null) ? safeTrim(txtPassword.getText()) : "";
            UserRole rol = selfEditMode ? lockedRole : (cmbRol != null && cmbRol.getValue() != null ? cmbRol.getValue() : UserRole.EMPLEADO);
            boolean activo = selfEditMode ? lockedActivo : (chkActivo != null && chkActivo.isSelected());
            SaveUserRequest request = new SaveUserRequest(editingId, empresaId, nombre, email, password, rol, activo);
            saveUserUseCase.save(request);
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
        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }

    private void loadEmpresas() {
        empresas.clear();
        String sql = "SELECT id, nombre FROM empresa ORDER BY nombre ASC";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                empresas.add(new EmpresaOption(rs.getLong("id"), rs.getString("nombre")));
            }
        } catch (Exception ex) {
            throw new RuntimeException("No se pudieron cargar las empresas", ex);
        }
        if (cmbEmpresa != null) {
            cmbEmpresa.setItems(empresas);
        }
    }

    private void applyPendingEmpresaSelectionIfAny() {
        if (cmbEmpresa == null || pendingEmpresaIdToSelect == null) {
            return;
        }

        EmpresaOption opt = empresas.stream().filter(e -> e.id == pendingEmpresaIdToSelect).findFirst().orElse(null);

        if (opt != null) {
            settingComboProgrammatically = true;
            cmbEmpresa.getSelectionModel().select(opt);
            selectedEmpresaId = opt.id;
            Platform.runLater(() -> settingComboProgrammatically = false);
        }
        pendingEmpresaIdToSelect = null;
    }

    private void enableComboSearchEmpresa(ComboBox<EmpresaOption> combo) {
        if (combo == null) {
            return;
        }

        combo.setEditable(true);

        combo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(EmpresaOption opt) {
                return opt == null ? "" : opt.nombre;
            }
            @Override
            public EmpresaOption fromString(String s) {
                if (s == null) {
                    return null;
                }
                String t = s.trim();
                if (t.isEmpty()) {
                    return null;
                }
                return empresas.stream().filter(e -> e.nombre != null && e.nombre.equalsIgnoreCase(t)).findFirst().orElse(null);
            }
        });

        empresasFiltradas = new FilteredList<>(empresas, e -> true);
        combo.setItems(empresasFiltradas);

        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EmpresaOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.nombre);
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(EmpresaOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.nombre);
            }
        });

        combo.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (settingComboProgrammatically) {
                return;
            }
            EmpresaOption selected = combo.getSelectionModel().getSelectedItem();
            String nt = (newText == null ? "" : newText).trim();
            if (selected != null && selected.nombre != null && selected.nombre.equalsIgnoreCase(nt)) {
                return;
            }
            String f = nt.toLowerCase(Locale.ROOT);
            settingComboProgrammatically = true;
            try {
                empresasFiltradas.setPredicate(opt
                        -> f.isEmpty() || (opt.nombre != null && opt.nombre.toLowerCase(Locale.ROOT).contains(f))
                );
            } finally {
                settingComboProgrammatically = false;
            }

            if (combo.isFocused() && !combo.isShowing()) {
                combo.show();
            }
        });

        combo.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                return;
            }
            EmpresaOption match = combo.getConverter().fromString(combo.getEditor().getText());
            settingComboProgrammatically = true;
            if (match != null) {
                combo.getSelectionModel().select(match);
                selectedEmpresaId = match.id;
            } else {
                combo.getSelectionModel().clearSelection();
            }
            settingComboProgrammatically = false;
        });
    }

    private void hideNode(Node n) {
        if (n == null) {
            return;
        }
        n.setVisible(false);
        n.setManaged(false);
    }

    private void showNode(Node n) {
        if (n == null) {
            return;
        }
        n.setVisible(true);
        n.setManaged(true);
    }

    private String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    public static class EmpresaOption {

        public final long id;
        public final String nombre;
        public EmpresaOption(long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
        @Override
        public String toString() {
            return nombre;
        }
    }
}
