package com.gearmind.presentation.controller;

import com.gearmind.application.company.ListEmpresasUseCase;
import com.gearmind.application.company.SaveEmpresaRequest;
import com.gearmind.application.company.SaveEmpresaUseCase;
import com.gearmind.domain.company.Empresa;
import com.gearmind.infrastructure.company.MySqlEmpresaRepository;
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

    private final ListEmpresasUseCase listEmpresasUseCase;
    private final SaveEmpresaUseCase saveEmpresaUseCase;

    private final ObservableList<Empresa> masterData = FXCollections.observableArrayList();
    private final ObservableList<Empresa> displayedData = FXCollections.observableArrayList();

    public EmpresasController() {
        MySqlEmpresaRepository repo = new MySqlEmpresaRepository();
        this.listEmpresasUseCase = new ListEmpresasUseCase(repo);
        this.saveEmpresaUseCase = new SaveEmpresaUseCase(repo);
    }

    @FXML
    private void initialize() {
        tblEmpresas.setItems(displayedData);
        tblEmpresas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCif.setCellValueFactory(new PropertyValueFactory<>("cif"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colCiudad.setCellValueFactory(new PropertyValueFactory<>("ciudad"));
        colProvincia.setCellValueFactory(new PropertyValueFactory<>("provincia"));
        colCp.setCellValueFactory(new PropertyValueFactory<>("cp"));

        colEstado.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().isActiva() ? "Activa" : "Inactiva")
        );

        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnToggle = new Button();
            private final HBox container = new HBox(8, btnEditar, btnToggle);

            {
                btnEditar.getStyleClass().add("tfx-btn-ghost");
                btnToggle.getStyleClass().add("tfx-btn-primary");

                btnEditar.setOnAction(e -> onEditar(getCurrentEmpresa()));
                btnToggle.setOnAction(e -> onToggleActiva(getCurrentEmpresa()));
            }

            private Empresa getCurrentEmpresa() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(Empresa empresa, boolean empty) {
                super.updateItem(empresa, empty);
                if (empty || empresa == null) {
                    setGraphic(null);
                } else {
                    btnToggle.setText(empresa.isActiva() ? "Desactivar" : "Activar");
                    setGraphic(container);
                }
            }
        });

        cmbPageSize.setItems(FXCollections.observableArrayList(10, 25, 50, 100));
        cmbPageSize.getSelectionModel().select(Integer.valueOf(25));
        cmbPageSize.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltro(txtBuscar != null ? txtBuscar.getText() : "")
        );

        cargarEmpresas();
        configurarBuscador();
        btnNueva.setOnAction(e -> onNuevaEmpresa());
    }

    private void cargarEmpresas() {
        List<Empresa> empresas = listEmpresasUseCase.execute();
        masterData.setAll(empresas);
        aplicarFiltro(txtBuscar != null ? txtBuscar.getText() : "");
    }

    private void configurarBuscador() {
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltro(newVal));
    }

    private void aplicarFiltro(String filtroTexto) {
        String filtro = filtroTexto == null ? "" : filtroTexto.toLowerCase(Locale.ROOT).trim();

        ObservableList<Empresa> filtradas = FXCollections.observableArrayList();
        for (Empresa e : masterData) {
            if (filtro.isEmpty() || coincideConFiltro(e, filtro)) {
                filtradas.add(e);
            }
        }

        int total = filtradas.size();
        int limit = (cmbPageSize != null && cmbPageSize.getValue() != null)
                ? cmbPageSize.getValue()
                : Integer.MAX_VALUE;

        List<Empresa> visible = filtradas.subList(0, Math.min(limit, total));
        displayedData.setAll(visible);

        if (lblResumen != null) {
            lblResumen.setText("Mostrando " + visible.size() + " de " + total + " empresas");
        }
    }

    private boolean coincideConFiltro(Empresa e, String filtro) {
        return contiene(e.getNombre(), filtro)
                || contiene(e.getCif(), filtro)
                || contiene(e.getTelefono(), filtro)
                || contiene(e.getEmail(), filtro)
                || contiene(e.getCiudad(), filtro)
                || contiene(e.getProvincia(), filtro)
                || contiene(e.getCp(), filtro);
    }

    private boolean contiene(String valor, String filtro) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(filtro);
    }

    @FXML
    private void onRefrescar() {
        cargarEmpresas();
    }

    private void onNuevaEmpresa() {
        openForm(null); // modo creación
    }

    private void onEditar(Empresa empresa) {
        if (empresa == null) return;
        openForm(empresa); // modo edición
    }

    private void onToggleActiva(Empresa empresa) {
        if (empresa == null) return;

        boolean nuevoEstado = !empresa.isActiva();

        SaveEmpresaRequest req = new SaveEmpresaRequest(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getCif(),
                empresa.getTelefono(),
                empresa.getEmail(),
                empresa.getDireccion(),
                empresa.getCiudad(),
                empresa.getProvincia(),
                empresa.getCp(),
                nuevoEstado
        );

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
            stage.setScene(new Scene(root));
            stage.initOwner(tblEmpresas.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            cargarEmpresas();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onBack() {
        try {
            Stage stage = (Stage) tblEmpresas.getScene().getWindow();

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
