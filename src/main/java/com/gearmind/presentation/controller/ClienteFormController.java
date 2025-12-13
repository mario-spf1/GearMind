package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.customer.SaveCustomerRequest;
import com.gearmind.application.customer.SaveCustomerUseCase;
import com.gearmind.domain.customer.Customer;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import com.gearmind.infrastructure.database.DataSourceFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

public class ClienteFormController {

    @FXML
    private Label lblTitulo;

    // ✅ importante: label separado para poder ocultarlo
    @FXML
    private Label lblEmpresa;

    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtTelefono;
    @FXML
    private TextArea txtNotas;

    @FXML
    private Button btnGuardar;

    @FXML
    private HBox boxEmpresa;
    @FXML
    private ComboBox<EmpresaOption> cmbEmpresa;

    private long selectedEmpresaId;
    private Long editingId = null;
    private boolean saved = false;

    private final SaveCustomerUseCase saveCustomerUseCase;
    private final ObservableList<EmpresaOption> empresas = FXCollections.observableArrayList();
    private FilteredList<EmpresaOption> empresasFiltradas;

    private final DataSource dataSource = DataSourceFactory.getDataSource();

    public ClienteFormController() {
        this.saveCustomerUseCase = new SaveCustomerUseCase(new MySqlCustomerRepository());
    }

    public boolean isSaved() {
        return saved;
    }

    public void initForNew() {
        editingId = null;
        if (lblTitulo != null) {
            lblTitulo.setText("Nuevo cliente");
        }

        if (AuthContext.isSuperAdmin()) {
            if (cmbEmpresa != null) {
                cmbEmpresa.getSelectionModel().clearSelection();
            }
            selectedEmpresaId = 0;
        } else {
            selectedEmpresaId = SessionManager.getInstance().getCurrentEmpresaId();
        }
    }

    public void initForEdit(Customer customer) {
        editingId = customer.getId();
        if (lblTitulo != null) {
            lblTitulo.setText("Editar cliente");
        }

        txtNombre.setText(customer.getNombre());
        txtEmail.setText(customer.getEmail());
        txtTelefono.setText(customer.getTelefono());
        txtNotas.setText(customer.getNotas());

        if (AuthContext.isSuperAdmin()) {
            selectedEmpresaId = customer.getEmpresaId();

            if (cmbEmpresa != null) {
                EmpresaOption opt = empresas.stream()
                        .filter(e -> e.id == selectedEmpresaId)
                        .findFirst()
                        .orElse(null);

                if (opt != null) {
                    cmbEmpresa.getSelectionModel().select(opt);
                }
            }
        } else {
            selectedEmpresaId = SessionManager.getInstance().getCurrentEmpresaId();
        }
    }

    @FXML
    private void initialize() {

        // Botón guardar activo solo si hay nombre
        if (btnGuardar != null) {
            btnGuardar.setDisable(true);
        }
        txtNombre.textProperty().addListener((obs, oldV, newV) -> {
            if (btnGuardar != null) {
                btnGuardar.setDisable(newV == null || newV.trim().isEmpty());
            }
        });

        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        // ✅ Admin/Empleado: ocultar label + combo de empresa
        if (!isSuperAdmin) {
            if (lblEmpresa != null) {
                lblEmpresa.setVisible(false);
                lblEmpresa.setManaged(false);
            }
            if (boxEmpresa != null) {
                boxEmpresa.setVisible(false);
                boxEmpresa.setManaged(false);
            }
            selectedEmpresaId = SessionManager.getInstance().getCurrentEmpresaId();
            return;
        }

        // ✅ SuperAdmin: mostrar y cargar empresas
        if (lblEmpresa != null) {
            lblEmpresa.setVisible(true);
            lblEmpresa.setManaged(true);
        }
        if (boxEmpresa != null) {
            boxEmpresa.setVisible(true);
            boxEmpresa.setManaged(true);
        }

        loadEmpresas();
        enableComboSearch(cmbEmpresa);

        if (cmbEmpresa != null) {
            cmbEmpresa.valueProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    selectedEmpresaId = n.id;
                }
            });
        }
    }

    @FXML
    private void onGuardar() {
        try {
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

            String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
            if (nombre.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "El nombre es obligatorio.").showAndWait();
                return;
            }

            String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
            if (!email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                new Alert(Alert.AlertType.WARNING, "El email no tiene un formato válido.").showAndWait();
                return;
            }

            String telefono = txtTelefono.getText() != null
                    ? txtTelefono.getText().trim().replaceAll("\\s+", " ")
                    : "";

            String notas = txtNotas.getText() != null ? txtNotas.getText().trim() : "";

            SaveCustomerRequest request = new SaveCustomerRequest(editingId, empresaId, nombre, email, telefono, notas);
            saveCustomerUseCase.save(request);

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
    }

    private void enableComboSearch(ComboBox<EmpresaOption> combo) {
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

                return empresas.stream()
                        .filter(e -> e.nombre != null && e.nombre.equalsIgnoreCase(t))
                        .findFirst()
                        .orElse(null);
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
            String f = (newText == null ? "" : newText).toLowerCase(Locale.ROOT).trim();
            empresasFiltradas.setPredicate(opt
                    -> f.isEmpty() || (opt.nombre != null && opt.nombre.toLowerCase(Locale.ROOT).contains(f))
            );
            if (!combo.isShowing()) {
                combo.show();
            }
        });

        combo.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                return;
            }

            EmpresaOption match = combo.getConverter().fromString(combo.getEditor().getText());
            if (match != null) {
                combo.getSelectionModel().select(match);
            } else {
                combo.getSelectionModel().clearSelection();
            }
        });
    }

    private static class EmpresaOption {

        final long id;
        final String nombre;

        EmpresaOption(long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }
}
