package com.gearmind.presentation.controller;

import com.gearmind.application.common.SessionManager;
import com.gearmind.application.customer.ActivateCustomerUseCase;
import com.gearmind.application.customer.DeactivateCustomerUseCase;
import com.gearmind.application.customer.ListCustomersUseCase;
import com.gearmind.domain.customer.Customer;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClientesController {

    @FXML
    private TableView<Customer> tblClientes;

    @FXML
    private TableColumn<Customer, String> colNombre;

    @FXML
    private TableColumn<Customer, String> colTelefono;

    @FXML
    private TableColumn<Customer, String> colEmail;

    @FXML
    private TableColumn<Customer, String> colNotas;

    @FXML
    private TableColumn<Customer, String> colEstado;

    @FXML
    private TableColumn<Customer, Customer> colAcciones;

    @FXML
    private TextField txtBuscar;

    @FXML
    private Button btnNuevoCliente;

    @FXML
    private ComboBox<Integer> cmbPageSize;

    @FXML
    private Label lblResumen;

    private final ListCustomersUseCase listCustomersUseCase;
    private final DeactivateCustomerUseCase deactivateCustomerUseCase;
    private final ActivateCustomerUseCase activateCustomerUseCase;

    private final ObservableList<Customer> masterData = FXCollections.observableArrayList();

    public ClientesController() {
        MySqlCustomerRepository repo = new MySqlCustomerRepository();
        this.listCustomersUseCase = new ListCustomersUseCase(repo);
        this.deactivateCustomerUseCase = new DeactivateCustomerUseCase(repo);
        this.activateCustomerUseCase = new ActivateCustomerUseCase(repo);
    }

    @FXML
    private void initialize() {
        tblClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        colNotas.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNotas())
        );
        colNotas.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String trimmed = item.length() > 40 ? item.substring(0, 37) + "..." : item;
                    setText(trimmed);
                    setTooltip(new Tooltip(item));
                }
            }
        });

        colEstado.setCellValueFactory(c ->new ReadOnlyObjectWrapper<>(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {

            private final Button btnEditar = new Button("Editar");
            private final Button btnToggle = new Button();
            private final HBox box = new HBox(5, btnEditar, btnToggle);

            {
                btnEditar.setOnAction(e -> {
                    Customer c = getItem();
                    if (c != null) {
                        openClienteForm(c);
                    }
                });

                btnToggle.setOnAction(e -> {
                    Customer c = getItem();
                    if (c != null) {
                        toggleCustomerActive(c);
                    }
                });
            }

            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setGraphic(null);
                } else {
                    btnToggle.setText(customer.isActivo() ? "Desactivar" : "Activar");
                    setGraphic(box);
                }
            }
        });
        colAcciones.setSortable(false);

        cmbPageSize.setItems(FXCollections.observableArrayList(5, 10, 25, 50));
        cmbPageSize.getSelectionModel().select(Integer.valueOf(10));
        cmbPageSize.valueProperty().addListener((obs, o, n) -> refreshTable());
        txtBuscar.textProperty().addListener((obs, o, n) -> refreshTable());
        setupRowDoubleClick();
        loadClientesFromDb();
    }

    private void loadClientesFromDb() {
        long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
        List<Customer> clientes = listCustomersUseCase.listByEmpresa(empresaId);
        masterData.setAll(clientes);
        refreshTable();
    }

    private void refreshTable() {
        String filtro = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);

        int limit = Optional.ofNullable(cmbPageSize.getValue()).orElse(Integer.MAX_VALUE);

        List<Customer> filtered = masterData.stream().filter(c -> matchesFilter(c, filtro)).sorted(Comparator.comparing(Customer::getNombre, String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList());

        int total = filtered.size();
        List<Customer> visible = filtered.subList(0, Math.min(limit, total));

        tblClientes.setItems(FXCollections.observableArrayList(visible));

        lblResumen.setText("Mostrando " + visible.size() + " de " + total + " clientes");
    }

    private boolean matchesFilter(Customer c, String filtro) {
        if (filtro.isEmpty()) return true;
        String nombre = safe(c.getNombre());
        String email = safe(c.getEmail());
        String telefono = safe(c.getTelefono());
        String notas = safe(c.getNotas());
        String estado = c.isActivo() ? "activo" : "inactivo";
        return nombre.contains(filtro) || email.contains(filtro) || telefono.contains(filtro) || notas.contains(filtro) || estado.contains(filtro);
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

    /**
     * Si está activo → desactiva.
     * Si está inactivo → activa.
     */
    private void toggleCustomerActive(Customer customer) {
        if (customer.isActivo()) {
            deactivateCustomer(customer);
        } else {
            activateCustomer(customer);
        }
    }

    private void deactivateCustomer(Customer customer) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Desactivar cliente");
        alert.setHeaderText("¿Desactivar cliente?");
        alert.setContentText("El cliente \"" + customer.getNombre() + "\" dejará de estar disponible en los listados.");

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
                deactivateCustomerUseCase.deactivate(customer.getId(), empresaId);
                loadClientesFromDb();
            }
        });
    }

    private void activateCustomer(Customer customer) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Activar cliente");
        alert.setHeaderText("¿Activar cliente?");
        alert.setContentText("El cliente \"" + customer.getNombre() + "\" volverá a estar disponible en los listados.");

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
                activateCustomerUseCase.activate(customer.getId(), empresaId);
                loadClientesFromDb();
            }
        });
    }

    @FXML
    private void onNuevoCliente() {
        openClienteForm(null);
    }

    @FXML
    private void onRefrescar() {
        loadClientesFromDb();
    }

    private void openClienteForm(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/ClienteFormView.fxml")
            );
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
                loadClientesFromDb();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onVolverHome() {
        try {
            Stage stage = (Stage) tblClientes.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/HomeView.fxml")
            );
            Parent root = loader.load();

            Scene scene = new Scene(
                    root,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight()
            );

            scene.getStylesheets().add(
                    getClass().getResource("/styles/theme.css").toExternalForm()
            );
            scene.getStylesheets().add(
                    getClass().getResource("/styles/components.css").toExternalForm()
            );

            stage.setTitle("GearMind — Inicio");
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
