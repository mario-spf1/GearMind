package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
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
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class VehiculosController {

    @FXML
    private TableView<Vehicle> tblVehiculos;
    @FXML
    private TableColumn<Vehicle, String> colEmpresa;
    @FXML
    private TableColumn<Vehicle, String> colCliente;
    @FXML
    private TableColumn<Vehicle, String> colMatricula;
    @FXML
    private TableColumn<Vehicle, String> colMarca;
    @FXML
    private TableColumn<Vehicle, String> colModelo;
    @FXML
    private TableColumn<Vehicle, Integer> colYear;
    @FXML
    private TableColumn<Vehicle, String> colVin;
    @FXML
    private TableColumn<Vehicle, Vehicle> colAcciones;
    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Label lblResumen;
    @FXML
    private Label lblTotalVehiculos;
    @FXML
    private HBox boxFilterEmpresa;
    @FXML
    private ComboBox<String> filterEmpresaCombo;
    @FXML
    private TextField filterClienteField;
    @FXML
    private TextField filterMatriculaField;
    @FXML
    private TextField filterMarcaField;
    @FXML
    private TextField filterModeloField;
    @FXML
    private TextField filterVinField;
    @FXML
    private TextField filterYearField;

    private final ObservableList<Vehicle> masterData = FXCollections.observableArrayList();
    private SmartTable<Vehicle> smartTable;

    private final ListVehiclesUseCase listVehiclesUseCase;

    public VehiculosController() {
        this.listVehiclesUseCase = new ListVehiclesUseCase(new MySqlVehicleRepository());
    }

    @FXML
    private void initialize() {

        boolean isSuperAdmin = AuthContext.isSuperAdmin();
        tblVehiculos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblVehiculos.setPlaceholder(new Label("No hay vehículos que mostrar."));

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
                colEmpresa.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getEmpresaNombre()))
                );
            }
        }

        colCliente.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getClienteNombre())));
        colMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colModelo.setCellValueFactory(new PropertyValueFactory<>("modelo"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("year"));
        colVin.setCellValueFactory(new PropertyValueFactory<>("vin"));
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

                btnEliminar.setOnAction(e -> showWarning("Eliminar vehículo pendiente de implementación.\n\n" + "Más adelante se comprobará si tiene datos asociados antes de borrarlo."));
            }

            @Override
            protected void updateItem(Vehicle vehiculo, boolean empty) {
                super.updateItem(vehiculo, empty);
                setGraphic(empty || vehiculo == null ? null : box);
            }
        });
        colAcciones.setSortable(false);

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

        smartTable = new SmartTable<>(tblVehiculos, masterData, null, cmbPageSize, lblResumen, "vehículos", null);

        tblVehiculos.setFixedCellSize(28);
        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblVehiculos.getFixedCellSize() + 2;
            tblVehiculos.setPrefHeight(tableHeight);
            tblVehiculos.setMinHeight(Region.USE_PREF_SIZE);
        });

        smartTable.addColumnFilter(filterClienteField, (v, text) -> safe(v.getClienteNombre()).contains(text));
        smartTable.addColumnFilter(filterMatriculaField, (v, text) -> safe(v.getMatricula()).contains(text));
        smartTable.addColumnFilter(filterMarcaField, (v, text) -> safe(v.getMarca()).contains(text));
        smartTable.addColumnFilter(filterModeloField, (v, text) -> safe(v.getModelo()).contains(text));
        smartTable.addColumnFilter(filterVinField, (v, text) -> safe(v.getVin()).contains(text));

        smartTable.addColumnFilter(filterYearField, (v, text) -> {
            String q = (text == null ? "" : text.trim());
            if (q.isEmpty()) {
                return true;
            }

            Integer y = v.getYear();
            if (y == null) {
                return false;
            }

            try {
                if (q.startsWith(">=")) {
                    int min = Integer.parseInt(q.substring(2).trim());
                    return y >= min;
                }
                if (q.startsWith("<=")) {
                    int max = Integer.parseInt(q.substring(2).trim());
                    return y <= max;
                }
                if (q.contains("-")) {
                    String[] parts = q.split("-", 2);
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    if (min > max) {
                        int tmp = min;
                        min = max;
                        max = tmp;
                    }
                    return y >= min && y <= max;
                }
                int exact = Integer.parseInt(q);
                return y == exact;

            } catch (NumberFormatException ex) {
                return true;
            }
        });

        if (isSuperAdmin && filterEmpresaCombo != null) {
            smartTable.addColumnFilter(filterEmpresaCombo, (v, selected) -> {
                if (selected == null || "Todas".equalsIgnoreCase(selected)) {
                    return true;
                }
                return safeRaw(v.getEmpresaNombre()).equalsIgnoreCase(selected);
            });
        }

        setupRowDoubleClick();
        loadVehiculosFromDb();
    }

    private void loadVehiculosFromDb() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();
        List<Vehicle> vehiculos = listVehiclesUseCase.execute();
        vehiculos.sort(Comparator.comparing(Vehicle::getMatricula, String.CASE_INSENSITIVE_ORDER));
        masterData.setAll(vehiculos);

        if (isSuperAdmin && filterEmpresaCombo != null) {
            var empresas = vehiculos.stream().map(Vehicle::getEmpresaNombre).filter(s -> s != null && !s.isBlank()).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            filterEmpresaCombo.setItems(FXCollections.observableArrayList(empresas));
            filterEmpresaCombo.getItems().add(0, "Todas");
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }

        smartTable.refresh();

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
    private void onRefrescar() {
        loadVehiculosFromDb();
    }

    @FXML
    private void onLimpiarFiltros() {
        if (filterClienteField != null) {
            filterClienteField.clear();
        }
        if (filterMatriculaField != null) {
            filterMatriculaField.clear();
        }
        if (filterMarcaField != null) {
            filterMarcaField.clear();
        }
        if (filterModeloField != null) {
            filterModeloField.clear();
        }
        if (filterVinField != null) {
            filterVinField.clear();
        }
        if (filterYearField != null) {
            filterYearField.clear();
        }

        if (AuthContext.isSuperAdmin() && filterEmpresaCombo != null) {
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }

        smartTable.refresh();
    }

    private void openVehiculoForm(Vehicle vehiculo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/VehiculoFormView.fxml"));
            Parent root = loader.load();
            VehiculoFormController controller = loader.getController();
            controller.setVehicle(vehiculo);
            controller.setOnSaved(this::loadVehiculosFromDb);
            Stage stage = new Stage();
            stage.initOwner(tblVehiculos.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(vehiculo == null ? "Nuevo vehículo" : "Editar vehículo");
            stage.setResizable(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/theme.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/components.css")).toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("No se pudo abrir el formulario de vehículo.\n\n" + e.getMessage());
        }
    }

    private void setupRowDoubleClick() {
        tblVehiculos.setRowFactory(tv -> {
            TableRow<Vehicle> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openVehiculoForm(row.getItem());
                }
            });
            return row;
        });
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private String safeRaw(String s) {
        return s == null ? "" : s;
    }

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
}
