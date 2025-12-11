package com.gearmind.presentation.controller;

import com.gearmind.application.vehicle.ListVehiclesUseCase;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.infrastructure.vehicle.MySqlVehicleRepository;
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class VehiculosController {

    @FXML
    private TableView<Vehicle> tblVehiculos;
    @FXML
    private TableColumn<Vehicle, String> colMatricula;
    @FXML
    private TableColumn<Vehicle, String> colMarca;
    @FXML
    private TableColumn<Vehicle, String> colModelo;
    @FXML
    private TableColumn<Vehicle, Integer> colYear;
    @FXML
    private TableColumn<Vehicle, String> colCliente;
    @FXML
    private TableColumn<Vehicle, Vehicle> colAcciones;

    @FXML
    private TextField txtBuscar;
    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Label lblResumen;
    @FXML
    private Label lblTotalVehiculos;   // <─ el del título “X vehículos registrados”

    private final ObservableList<Vehicle> masterData = FXCollections.observableArrayList();
    private SmartTable<Vehicle> smartTable;

    private final ListVehiclesUseCase listVehiclesUseCase;

    public VehiculosController() {
        this.listVehiclesUseCase = new ListVehiclesUseCase(new MySqlVehicleRepository());
    }

    @FXML
    private void initialize() {
        tblVehiculos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colModelo.setCellValueFactory(new PropertyValueFactory<>("modelo"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("year"));

        colCliente.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getClienteNombre())
        );

        // Columna Acciones: Editar + Eliminar (placeholder)
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {

            private final Button btnEditar = new Button("Editar");
            private final Button btnEliminar = new Button("Eliminar");
            private final HBox box = new HBox(8, btnEditar, btnEliminar);

            {
                btnEditar.getStyleClass().add("tfx-icon-btn");
                btnEliminar.getStyleClass().add("tfx-icon-btn-danger");

                btnEditar.setTooltip(new Tooltip("Editar vehículo"));
                btnEliminar.setTooltip(new Tooltip("Eliminar vehículo"));

                btnEditar.setOnAction(e -> {
                    Vehicle v = getItem();
                    if (v != null) {
                        openVehiculoForm(v);
                    }
                });

                btnEliminar.setOnAction(e -> {
                    Vehicle v = getItem();
                    if (v != null) {
                        // TODO: lógica real de borrado
                        showWarning("Eliminar vehículo pendiente de implementación.\n\n" +
                                "Más adelante se comprobará si tiene datos asociados antes de borrarlo.");
                    }
                });
            }

            @Override
            protected void updateItem(Vehicle vehiculo, boolean empty) {
                super.updateItem(vehiculo, empty);
                if (empty || vehiculo == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
        colAcciones.setSortable(false);

        // Page size igual que en Clientes
        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(5, 10, 25, 50));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(10));
        }

        // SmartTable exactamente igual que en Clientes
        smartTable = new SmartTable<>(tblVehiculos, masterData, txtBuscar,
                cmbPageSize, lblResumen, "vehículos", this::matchesGlobalFilter);

        tblVehiculos.setFixedCellSize(28);
        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblVehiculos.getFixedCellSize() + 2;
            tblVehiculos.setPrefHeight(tableHeight);
        });

        setupRowDoubleClick();
        loadVehiculosFromDb();
    }

    private void loadVehiculosFromDb() {
        List<Vehicle> vehiculos = listVehiclesUseCase.execute();

        // Orden por matrícula
        vehiculos.sort(Comparator.comparing(Vehicle::getMatricula, String.CASE_INSENSITIVE_ORDER));

        masterData.setAll(vehiculos);
        smartTable.refresh();

        // texto tipo "5 vehículos registrados", igual que en Clientes
        if (lblTotalVehiculos != null) {
            int total = masterData.size();
            lblTotalVehiculos.setText(total + (total == 1 ? " vehículo registrado" : " vehículos registrados"));
        }
    }

    @FXML
    private void onNuevo() {
        openVehiculoForm(null);
    }

    @FXML
    private void onEditar() {
        Vehicle selected = tblVehiculos.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selecciona un vehículo para editar.");
            return;
        }
        openVehiculoForm(selected);
    }

    @FXML
    private void onRefrescar() {
        loadVehiculosFromDb();
    }

    private void openVehiculoForm(Vehicle vehiculo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/VehiculoFormView.fxml")
            );
            Parent root = loader.load();

            VehiculoFormController controller = loader.getController();
            controller.setVehicle(vehiculo);
            controller.setOnSaved(this::loadVehiculosFromDb);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(vehiculo == null ? "Nuevo vehículo" : "Editar vehículo");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showError("No se pudo abrir el formulario de vehículo.");
        }
    }

    private void setupRowDoubleClick() {
        tblVehiculos.setRowFactory(tv -> {
            TableRow<Vehicle> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Vehicle v = row.getItem();
                    openVehiculoForm(v);
                }
            });
            return row;
        });
    }

    /** Filtro global (campo Buscar...) */
    private boolean matchesGlobalFilter(Vehicle v, String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return true;
        }
        String f = filtro.toLowerCase(Locale.ROOT);

        String matricula = safe(v.getMatricula());
        String marca     = safe(v.getMarca());
        String modelo    = safe(v.getModelo());
        String cliente   = safe(v.getClienteNombre());

        return matricula.contains(f) || marca.contains(f) || modelo.contains(f) || cliente.contains(f);
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
}
