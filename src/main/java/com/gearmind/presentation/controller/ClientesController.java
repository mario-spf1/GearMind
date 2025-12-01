package com.gearmind.presentation.controller;

import com.gearmind.application.common.SessionManager;
import com.gearmind.application.customer.ListCustomersUseCase;
import com.gearmind.domain.customer.Customer;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class ClientesController {

    @FXML
    private TableView<Customer> tblClientes;

    @FXML
    private TableColumn<Customer, String> colNombre;

    @FXML
    private TableColumn<Customer, String> colEmail;

    @FXML
    private TableColumn<Customer, String> colTelefono;

    @FXML
    private TextField txtBuscar;

    @FXML
    private Button btnNuevoCliente;

    private final ListCustomersUseCase listCustomersUseCase;

    private ObservableList<Customer> masterData;

    public ClientesController() {
        this.listCustomersUseCase = new ListCustomersUseCase(
                new MySqlCustomerRepository()
        );
    }

    @FXML
    private void initialize() {
        tblClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        masterData = FXCollections.observableArrayList();
        loadClientes();
        setupSearch();
        setupRowDoubleClick();
    }

    private void loadClientes() {
        long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
        List<Customer> clientes = listCustomersUseCase.listByEmpresa(empresaId);

        if (masterData == null) {
            masterData = FXCollections.observableArrayList(clientes);
        } else {
            masterData.setAll(clientes);
        }
    }

    private void setupSearch() {
        FilteredList<Customer> filtered = new FilteredList<>(masterData, c -> true);

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtro = newVal == null ? "" : newVal.trim().toLowerCase(Locale.ROOT);
            if (filtro.isEmpty()) {
                filtered.setPredicate(c -> true);
            } else {
                filtered.setPredicate(buildFilterPredicate(filtro));
            }
        });

        SortedList<Customer> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tblClientes.comparatorProperty());
        tblClientes.setItems(sorted);
    }

    private Predicate<Customer> buildFilterPredicate(String filtro) {
        return c -> {
            if (c == null) {
                return false;
            }
            String nombre = safe(c.getNombre());
            String email = safe(c.getEmail());
            String telefono = safe(c.getTelefono());
            return nombre.contains(filtro) || email.contains(filtro) || telefono.contains(filtro);
        };
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private void setupRowDoubleClick() {
        tblClientes.setRowFactory(tv -> {
            TableRow<Customer> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Customer c = row.getItem();
                    openClienteForm(c);
                }
            });
            return row;
        });
    }

    @FXML
    private void onNuevoCliente() {
        openClienteForm(null);
    }

    private void openClienteForm(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ClienteFormView.fxml"));
            Parent root = loader.load();

            ClienteFormController controller = loader.getController();
            if (customer == null) {
                controller.initForNew();
            } else {
                controller.initForEdit(customer);
            }

            Stage dialog = new Stage();
            dialog.initOwner(tblClientes.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle(customer == null ? "Nuevo cliente" : "Editar cliente");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

            if (controller.isSaved()) {
                loadClientes();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onVolverHome() {
        try {
            Stage stage = (Stage) tblClientes.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/HomeView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());

            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());

            stage.setTitle("GearMind — Inicio");
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
