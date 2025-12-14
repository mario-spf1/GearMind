package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.customer.ActivateCustomerUseCase;
import com.gearmind.application.customer.DeactivateCustomerUseCase;
import com.gearmind.application.customer.ListCustomersUseCase;
import com.gearmind.domain.customer.Customer;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import com.gearmind.presentation.table.SmartTable;
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
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ClientesController {

    @FXML
    private TableView<Customer> tblClientes;
    @FXML
    private TableColumn<Customer, String> colEmpresa;
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
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Button btnNuevoCliente;
    @FXML
    private Label lblHeaderInfo;

    @FXML
    private TextField filterNombreField;
    @FXML
    private TextField filterTelefonoField;
    @FXML
    private TextField filterEmailField;
    @FXML
    private ComboBox<String> filterEstadoCombo;

    @FXML
    private ComboBox<String> filterEmpresaCombo;
    @FXML
    private HBox boxFilterEmpresa;
    @FXML
    private HBox boxFilterEstado;

    @FXML
    private Label lblResumen;

    private final ListCustomersUseCase listCustomersUseCase;
    private final DeactivateCustomerUseCase deactivateCustomerUseCase;
    private final ActivateCustomerUseCase activateCustomerUseCase;

    private final ObservableList<Customer> masterData = FXCollections.observableArrayList();
    private SmartTable<Customer> smartTable;

    public ClientesController() {
        MySqlCustomerRepository repo = new MySqlCustomerRepository();
        this.listCustomersUseCase = new ListCustomersUseCase(repo);
        this.deactivateCustomerUseCase = new DeactivateCustomerUseCase(repo);
        this.activateCustomerUseCase = new ActivateCustomerUseCase(repo);
    }

    @FXML
    private void initialize() {

        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        tblClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblClientes.setPlaceholder(new Label("No hay clientes que mostrar."));

        // ===== Empresa (solo SuperAdmin) =====
        if (!isSuperAdmin) {
            if (colEmpresa != null) {
                colEmpresa.setVisible(false);
            }
            if (boxFilterEmpresa != null) {
                boxFilterEmpresa.setVisible(false);
                boxFilterEmpresa.setManaged(false);
            }
        } else {
            if (colEmpresa != null) {
                colEmpresa.setCellValueFactory(c
                        -> new SimpleStringProperty(safeRaw(c.getValue().getEmpresaNombre()))
                );
            }
        }

        // ===== Columnas base =====
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        colNotas.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNotas()));
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

        colEstado.setCellValueFactory(c
                -> new ReadOnlyObjectWrapper<>(c.getValue().isActivo() ? "Activo" : "Inactivo")
        );
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-badge-success", "tfx-badge-danger");

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().add("tfx-badge");
                    getStyleClass().add("Activo".equalsIgnoreCase(item) ? "tfx-badge-success" : "tfx-badge-danger");
                }
            }
        });

        // ===== Acciones =====
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {

            private final Button btnEditar = new Button("Editar");
            private final Button btnToggle = new Button();
            private final HBox box = new HBox(8, btnEditar, btnToggle);

            {
                btnEditar.getStyleClass().add("tfx-icon-btn");
                btnToggle.getStyleClass().add("tfx-icon-btn");

                btnEditar.setTooltip(new Tooltip("Editar cliente"));
                btnToggle.setTooltip(new Tooltip("Activar/Desactivar"));

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
                    return;
                }

                btnToggle.getStyleClass().removeAll("tfx-icon-btn-danger", "tfx-icon-btn-success");

                if (customer.isActivo()) {
                    btnToggle.setText("Desactivar");
                    btnToggle.getStyleClass().add("tfx-icon-btn-danger");
                    btnToggle.setTooltip(new Tooltip("Desactivar cliente"));
                } else {
                    btnToggle.setText("Activar");
                    btnToggle.getStyleClass().add("tfx-icon-btn-success");
                    btnToggle.setTooltip(new Tooltip("Activar cliente"));
                }

                setGraphic(box);
            }
        });
        colAcciones.setSortable(false);

        // ===== Page Size =====
        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(5, 15, 25, 0));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(15));

            var converter = new javafx.util.StringConverter<Integer>() {
                @Override
                public String toString(Integer value) {
                    if (value == null) {
                        return "";
                    }
                    return value == 0 ? "Todos" : String.valueOf(value);
                }

                @Override
                public Integer fromString(String s) {
                    if (s == null) {
                        return 15;
                    }
                    s = s.trim();
                    return "Todos".equalsIgnoreCase(s) ? 0 : Integer.valueOf(s);
                }
            };

            cmbPageSize.setConverter(converter);
            cmbPageSize.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : (item == 0 ? "Todos" : String.valueOf(item)));
                }
            });
            cmbPageSize.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : (item == 0 ? "Todos" : String.valueOf(item)));
                }
            });
        }

        // ===== SmartTable =====
        smartTable = new SmartTable<>(tblClientes, masterData, null, cmbPageSize, lblResumen, "clientes", null);

        tblClientes.setFixedCellSize(28);
        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblClientes.getFixedCellSize() + 2;
            tblClientes.setPrefHeight(tableHeight);
            tblClientes.setMinHeight(Region.USE_PREF_SIZE);
        });

        // ===== Filtros =====
        smartTable.addColumnFilter(filterNombreField, (c, text) -> safe(c.getNombre()).contains(text));
        smartTable.addColumnFilter(filterTelefonoField, (c, text) -> safe(c.getTelefono()).contains(text));
        smartTable.addColumnFilter(filterEmailField, (c, text) -> safe(c.getEmail()).contains(text));

        if (filterEstadoCombo != null) {
            filterEstadoCombo.setItems(FXCollections.observableArrayList("Todos", "Activo", "Inactivo"));
            filterEstadoCombo.getSelectionModel().select("Todos");

            smartTable.addColumnFilter(filterEstadoCombo, (c, selected) -> {
                if (selected == null || "Todos".equalsIgnoreCase(selected)) {
                    return true;
                }
                return "Activo".equalsIgnoreCase(selected) ? c.isActivo() : !c.isActivo();
            });
        }

        if (isSuperAdmin && filterEmpresaCombo != null) {
            // OJO: el filtro se define UNA sola vez aquí
            smartTable.addColumnFilter(filterEmpresaCombo, (c, selected) -> {
                if (selected == null || "Todas".equalsIgnoreCase(selected)) {
                    return true;
                }
                return safeRaw(c.getEmpresaNombre()).equalsIgnoreCase(selected);
            });
        }

        setupRowDoubleClick();
        loadClientesFromDb();
    }

    private void loadClientesFromDb() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        List<Customer> clientes;
        if (isSuperAdmin) {
            clientes = listCustomersUseCase.listAllWithEmpresa();
        } else {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            clientes = listCustomersUseCase.listByEmpresa(empresaId);
        }

        clientes.sort(Comparator.comparing(Customer::getNombre, String.CASE_INSENSITIVE_ORDER));
        masterData.setAll(clientes);

        // Cargar opciones de empresa SOLO para SuperAdmin (sin re-addColumnFilter)
        if (isSuperAdmin && filterEmpresaCombo != null) {
            var empresas = clientes.stream()
                    .map(Customer::getEmpresaNombre)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();

            filterEmpresaCombo.setItems(FXCollections.observableArrayList(empresas));
            filterEmpresaCombo.getItems().add(0, "Todas");
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }

        smartTable.refresh();

        if (lblHeaderInfo != null) {
            lblHeaderInfo.setText(masterData.size() + " clientes registrados");
        }
    }

    private void setupRowDoubleClick() {
        tblClientes.setRowFactory(tv -> {
            TableRow<Customer> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openClienteForm(row.getItem());
                }
            });
            return row;
        });
    }

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
                deactivateCustomerUseCase.deactivate(customer.getId(), customer.getEmpresaId());
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
                activateCustomerUseCase.activate(customer.getId(), customer.getEmpresaId());
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ClienteFormView.fxml"));
            Parent root = loader.load();

            ClienteFormController controller = loader.getController();
            if (customer == null) {
                controller.initForNew();
            } else {
                controller.initForEdit(customer);
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(java.util.Objects.requireNonNull(getClass().getResource("/styles/theme.css")).toExternalForm());
            scene.getStylesheets().add(java.util.Objects.requireNonNull(getClass().getResource("/styles/components.css")).toExternalForm());
            Stage stage = new Stage();
            stage.initOwner(tblClientes.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(customer == null ? "Nuevo cliente" : "Editar cliente");
            stage.setResizable(false);
            stage.setScene(scene);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadClientesFromDb();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario.\n\n" + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onLimpiarFiltros() {
        if (filterNombreField != null) {
            filterNombreField.clear();
        }
        if (filterTelefonoField != null) {
            filterTelefonoField.clear();
        }
        if (filterEmailField != null) {
            filterEmailField.clear();
        }

        if (filterEstadoCombo != null) {
            filterEstadoCombo.getSelectionModel().select("Todos");
        }

        if (AuthContext.isSuperAdmin() && filterEmpresaCombo != null) {
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }

        smartTable.refresh();
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private String safeRaw(String s) {
        return s == null ? "" : s;
    }
}
