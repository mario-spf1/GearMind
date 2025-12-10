package com.gearmind.presentation.controller;

import com.gearmind.application.company.ListEmpresasUseCase;
import com.gearmind.application.company.SaveEmpresaRequest;
import com.gearmind.application.company.SaveEmpresaUseCase;
import com.gearmind.domain.company.Empresa;
import com.gearmind.infrastructure.company.MySqlEmpresaRepository;
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
import java.util.List;
import java.util.Locale;

public class EmpresasController {

    @FXML
    private TableView<Empresa> tblEmpresas;
    @FXML
    private TableColumn<Empresa, String> colNombre;
    @FXML
    private TableColumn<Empresa, String> colCif;
    @FXML
    private TableColumn<Empresa, String> colTelefono;
    @FXML
    private TableColumn<Empresa, String> colEmail;
    @FXML
    private TableColumn<Empresa, String> colCiudad;
    @FXML
    private TableColumn<Empresa, String> colProvincia;
    @FXML
    private TableColumn<Empresa, String> colCp;
    @FXML
    private TableColumn<Empresa, String> colEstado;
    @FXML
    private TableColumn<Empresa, Empresa> colAcciones;
    @FXML
    private Button btnNueva;
    @FXML
    private TextField txtBuscar;
    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Label lblResumen;
    @FXML
    private TextField filterNombreField;
    @FXML
    private TextField filterCifField;
    @FXML
    private TextField filterTelefonoField;
    @FXML
    private TextField filterEmailField;
    @FXML
    private TextField filterCiudadField;
    @FXML
    private TextField filterProvinciaField;
    @FXML
    private TextField filterCpField;
    @FXML
    private TextField filterEstadoField;

    private final ListEmpresasUseCase listEmpresasUseCase;
    private final SaveEmpresaUseCase saveEmpresaUseCase;
    private final ObservableList<Empresa> masterData = FXCollections.observableArrayList();
    private SmartTable<Empresa> smartTable;

    public EmpresasController() {
        MySqlEmpresaRepository repo = new MySqlEmpresaRepository();
        this.listEmpresasUseCase = new ListEmpresasUseCase(repo);
        this.saveEmpresaUseCase = new SaveEmpresaUseCase(repo);
    }

    @FXML
    private void initialize() {
        tblEmpresas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCif.setCellValueFactory(new PropertyValueFactory<>("cif"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colCiudad.setCellValueFactory(new PropertyValueFactory<>("ciudad"));
        colProvincia.setCellValueFactory(new PropertyValueFactory<>("provincia"));
        colCp.setCellValueFactory(new PropertyValueFactory<>("cp"));

        // Estado con badge como en Clientes/Usuarios
        colEstado.setCellValueFactory(cd
                -> new SimpleStringProperty(cd.getValue().isActiva() ? "Activa" : "Inactiva"));
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
                    if ("Activa".equalsIgnoreCase(item)) {
                        getStyleClass().add("tfx-badge-success");
                    } else {
                        getStyleClass().add("tfx-badge-danger");
                    }
                }
            }
        });

        setupAccionesColumn();
        setupRowDoubleClick();

        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(10, 25, 50, 100));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(25));
        }

        // SmartTable como en Clientes/Usuarios
        smartTable = new SmartTable<>(tblEmpresas, masterData, txtBuscar, cmbPageSize, lblResumen, "empresas", this::matchesGlobalFilter);

        tblEmpresas.setFixedCellSize(28);
        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblEmpresas.getFixedCellSize() + 2;
            tblEmpresas.setPrefHeight(tableHeight);
        });

        smartTable.addColumnFilter(filterNombreField, (e, text) -> safe(e.getNombre()).contains(text));
        smartTable.addColumnFilter(filterCifField, (e, text) -> safe(e.getCif()).contains(text));
        smartTable.addColumnFilter(filterTelefonoField, (e, text) -> safe(e.getTelefono()).contains(text));
        smartTable.addColumnFilter(filterEmailField, (e, text) -> safe(e.getEmail()).contains(text));
        smartTable.addColumnFilter(filterCiudadField, (e, text) -> safe(e.getCiudad()).contains(text));
        smartTable.addColumnFilter(filterProvinciaField, (e, text) -> safe(e.getProvincia()).contains(text));
        smartTable.addColumnFilter(filterCpField, (e, text) -> safe(e.getCp()).contains(text));
        smartTable.addColumnFilter(filterEstadoField, (e, text) -> (e.isActiva() ? "activa" : "inactiva").toLowerCase(Locale.ROOT).contains(text));
        cargarEmpresas();
    }

    private void setupAccionesColumn() {
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {

            private final Button btnEditar = new Button("Editar");
            private final Button btnToggle = new Button();
            private final HBox container = new HBox(8, btnEditar, btnToggle);

            {
                btnEditar.getStyleClass().add("tfx-icon-btn");
                btnToggle.getStyleClass().add("tfx-icon-btn");
                btnEditar.setTooltip(new Tooltip("Editar empresa"));
                btnToggle.setTooltip(new Tooltip("Activar/Desactivar"));

                btnEditar.setOnAction(e -> {
                    Empresa emp = getItem();
                    if (emp != null) {
                        onEditar(emp);
                    }
                });

                btnToggle.setOnAction(e -> {
                    Empresa emp = getItem();
                    if (emp != null) {
                        onToggleActiva(emp);
                    }
                });
            }

            @Override
            protected void updateItem(Empresa empresa, boolean empty) {
                super.updateItem(empresa, empty);
                if (empty || empresa == null) {
                    setGraphic(null);
                } else {
                    btnToggle.getStyleClass().removeAll("tfx-icon-btn-danger", "tfx-icon-btn-success");

                    if (empresa.isActiva()) {
                        btnToggle.setText("Desactivar");
                        btnToggle.getStyleClass().add("tfx-icon-btn-danger");
                        btnToggle.setTooltip(new Tooltip("Desactivar empresa"));
                    } else {
                        btnToggle.setText("Activar");
                        btnToggle.getStyleClass().add("tfx-icon-btn-success");
                        btnToggle.setTooltip(new Tooltip("Activar empresa"));
                    }

                    setGraphic(container);
                }
            }
        });
        colAcciones.setSortable(false);
    }

    private void setupRowDoubleClick() {
        tblEmpresas.setRowFactory(tv -> {
            TableRow<Empresa> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Empresa e = row.getItem();
                    onEditar(e);
                }
            });
            return row;
        });
    }

    private void cargarEmpresas() {
        List<Empresa> empresas = listEmpresasUseCase.execute();
        masterData.setAll(empresas);
        smartTable.refresh();
    }

    private boolean matchesGlobalFilter(Empresa e, String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return true;
        }
        String f = filtro.toLowerCase(Locale.ROOT);
        String nombre = safe(e.getNombre());
        String cif = safe(e.getCif());
        String telefono = safe(e.getTelefono());
        String email = safe(e.getEmail());
        String ciudad = safe(e.getCiudad());
        String provincia = safe(e.getProvincia());
        String cp = safe(e.getCp());
        String estado = e.isActiva() ? "activa" : "inactiva";

        return nombre.contains(f) || cif.contains(f) || telefono.contains(f) || email.contains(f) || ciudad.contains(f) || provincia.contains(f) || cp.contains(f) || estado.contains(f);
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    @FXML
    private void onRefrescar() {
        cargarEmpresas();
    }

    @FXML
    private void onNuevaEmpresa() {
        openForm(null);
    }

    private void onEditar(Empresa empresa) {
        if (empresa != null) {
            openForm(empresa);
        }
    }

    private void onToggleActiva(Empresa empresa) {
        if (empresa == null) {
            return;
        }

        boolean nuevoEstado = !empresa.isActiva();

        SaveEmpresaRequest req = new SaveEmpresaRequest(empresa.getId(), empresa.getNombre(), empresa.getCif(), empresa.getTelefono(), empresa.getEmail(), empresa.getDireccion(), empresa.getCiudad(), empresa.getProvincia(), empresa.getCp(), nuevoEstado);
        saveEmpresaUseCase.execute(req);
        cargarEmpresas();
    }

    private void openForm(Empresa empresa) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/EmpresaFormView.fxml"));
            Parent root = loader.load();
            EmpresaFormController controller = loader.getController();
            controller.setEmpresa(empresa);
            Stage stage = new Stage();
            stage.setTitle(empresa == null ? "Nueva empresa" : "Editar empresa");
            stage.initOwner(tblEmpresas.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            cargarEmpresas();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onLimpiarFiltros() {
        if (txtBuscar != null) {
            txtBuscar.clear();
        }
        if (filterNombreField != null) {
            filterNombreField.clear();
        }
        if (filterCifField != null) {
            filterCifField.clear();
        }
        if (filterTelefonoField != null) {
            filterTelefonoField.clear();
        }
        if (filterEmailField != null) {
            filterEmailField.clear();
        }
        if (filterCiudadField != null) {
            filterCiudadField.clear();
        }
        if (filterProvinciaField != null) {
            filterProvinciaField.clear();
        }
        if (filterCpField != null) {
            filterCpField.clear();
        }
        if (filterEstadoField != null) {
            filterEstadoField.clear();
        }
        smartTable.refresh();
    }
}
