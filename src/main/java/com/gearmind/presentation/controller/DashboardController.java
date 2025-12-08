package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.customer.ListCustomersUseCase;
import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DashboardController {

    // Labels métricas principales
    @FXML private Label lblEmpresaTitulo;
    @FXML private Label lblTotalClientes;
    @FXML private Label lblTotalVehiculos;
    @FXML private Label lblReparacionesAbiertas;
    @FXML private Label lblTareasPendientes;

    // Tabla últimos clientes
    @FXML private TableView<Customer> tblUltimosClientes;
    @FXML private TableColumn<Customer, String> colCliNombre;
    @FXML private TableColumn<Customer, String> colCliTelefono;
    @FXML private TableColumn<Customer, String> colCliEmail;
    @FXML private Label lblUltimosClientesInfo;

    // Resumen de sesión
    @FXML private Label lblUsuarioInfo;
    @FXML private Label lblRolInfo;
    @FXML private Label lblEmpresaInfo;

    private final ListCustomersUseCase listCustomersUseCase;

    public DashboardController() {
        this.listCustomersUseCase = new ListCustomersUseCase(
                new MySqlCustomerRepository()
        );
    }

    @FXML
    public void initialize() {
        setupSessionSummary();
        setupClientesTable();
        loadClientesData();
        setupPlaceholders();
    }

    private void setupSessionSummary() {
        if (!AuthContext.isLoggedIn()) {
            if (lblEmpresaTitulo != null) {
                lblEmpresaTitulo.setText("Resumen del taller");
            }
            if (lblUsuarioInfo != null) {
                lblUsuarioInfo.setText("No hay usuario en sesión");
            }
            return;
        }

        User user = AuthContext.getCurrentUser();
        UserRole role = AuthContext.getRole();
        String empresaNombre = AuthContext.getEmpresaNombre();

        if (lblEmpresaTitulo != null) {
            if (empresaNombre != null && !empresaNombre.isBlank()) {
                lblEmpresaTitulo.setText("Resumen de " + empresaNombre);
            } else {
                lblEmpresaTitulo.setText("Resumen del taller");
            }
        }

        if (lblUsuarioInfo != null) {
            lblUsuarioInfo.setText("Usuario: " + user.getNombre() + " (" + user.getEmail() + ")");
        }

        if (lblRolInfo != null) {
            String rolTexto = switch (role) {
                case SUPER_ADMIN -> "Super administrador";
                case ADMIN       -> "Administrador";
                case EMPLEADO    -> "Empleado";
            };
            lblRolInfo.setText("Rol: " + rolTexto);
        }

        if (lblEmpresaInfo != null) {
            if (empresaNombre != null && !empresaNombre.isBlank()) {
                lblEmpresaInfo.setText("Empresa: " + empresaNombre + " (ID " + AuthContext.getEmpresaId() + ")");
            } else {
                lblEmpresaInfo.setText("Empresa asociada no definida");
            }
        }
    }

    private void setupClientesTable() {
        if (tblUltimosClientes == null) {
            return;
        }

        colCliNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCliTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCliEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        tblUltimosClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadClientesData() {
        try {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            List<Customer> all = listCustomersUseCase.listByEmpresa(empresaId);

            // Conteo real de clientes
            if (lblTotalClientes != null) {
                lblTotalClientes.setText(String.valueOf(all.size()));
            }

            // Elegimos hasta 5 clientes "más recientes"
            // De momento los ordenamos alfabéticamente como placeholder,
            // cuando tengas fecha de creación, sustituyes el criterio.
            List<Customer> ultimos = all.stream()
                    .sorted(Comparator.comparing(Customer::getNombre,
                            String.CASE_INSENSITIVE_ORDER))
                    .limit(5)
                    .collect(Collectors.toList());

            if (tblUltimosClientes != null) {
                tblUltimosClientes.setItems(FXCollections.observableArrayList(ultimos));
            }

            if (lblUltimosClientesInfo != null) {
                if (all.isEmpty()) {
                    lblUltimosClientesInfo.setText("Todavía no hay clientes registrados.");
                } else if (all.size() <= 5) {
                    lblUltimosClientesInfo.setText("Mostrando todos los clientes (" + all.size() + ").");
                } else {
                    lblUltimosClientesInfo.setText("Mostrando los últimos " + ultimos.size()
                            + " de " + all.size() + " clientes.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (lblTotalClientes != null) {
                lblTotalClientes.setText("—");
            }
            if (lblUltimosClientesInfo != null) {
                lblUltimosClientesInfo.setText("No se pudieron cargar los clientes.");
            }
        }
    }

    private void setupPlaceholders() {
        if (lblTotalVehiculos != null) {
            lblTotalVehiculos.setText("—");
        }
        if (lblReparacionesAbiertas != null) {
            lblReparacionesAbiertas.setText("—");
        }
        if (lblTareasPendientes != null) {
            lblTareasPendientes.setText("—");
        }
    }
}
