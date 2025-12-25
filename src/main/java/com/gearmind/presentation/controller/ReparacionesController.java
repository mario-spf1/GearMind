package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.application.repair.ChangeRepairStatusUseCase;
import com.gearmind.application.repair.ListRepairsUseCase;
import com.gearmind.application.repair.SaveRepairUseCase;
import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.repair.RepairStatus;
import com.gearmind.infrastructure.repair.MySqlRepairRepository;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ReparacionesController {

    @FXML
    private TableView<Repair> tblReparaciones;
    @FXML
    private TableColumn<Repair, String> colEmpresa;
    @FXML
    private TableColumn<Repair, String> colCliente;
    @FXML
    private TableColumn<Repair, String> colVehiculo;
    @FXML
    private TableColumn<Repair, String> colCita;
    @FXML
    private TableColumn<Repair, String> colDescripcion;
    @FXML
    private TableColumn<Repair, String> colEstado;
    @FXML
    private TableColumn<Repair, String> colImporteEstimado;
    @FXML
    private TableColumn<Repair, String> colImporteFinal;
    @FXML
    private TableColumn<Repair, Repair> colAcciones;
    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Label lblResumen;
    @FXML
    private Label lblHeaderInfo;
    @FXML
    private Button btnNuevaReparacion;
    @FXML
    private TextField filterClienteField;
    @FXML
    private TextField filterVehiculoField;
    @FXML
    private TextField filterCitaField;
    @FXML
    private TextField filterDescripcionField;
    @FXML
    private ComboBox<String> filterEstadoCombo;
    @FXML
    private ComboBox<String> filterEmpresaCombo;
    @FXML
    private HBox boxFilterEmpresa;

    private final ObservableList<Repair> masterData = FXCollections.observableArrayList();
    private SmartTable<Repair> smartTable;

    private final ListRepairsUseCase listRepairsUseCase;
    private final SaveRepairUseCase saveRepairUseCase;
    private final ChangeRepairStatusUseCase changeRepairStatusUseCase;

    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.getDefault()));

    public ReparacionesController() {
        MySqlRepairRepository repo = new MySqlRepairRepository();
        this.listRepairsUseCase = new ListRepairsUseCase(repo);
        this.saveRepairUseCase = new SaveRepairUseCase(repo);
        this.changeRepairStatusUseCase = new ChangeRepairStatusUseCase(repo);
    }

    @FXML
    private void initialize() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        tblReparaciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblReparaciones.setPlaceholder(new Label("No hay reparaciones que mostrar."));

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
                colEmpresa.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getEmpresaNombre())));
            }
        }

        colCliente.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getClienteNombre())));
        colVehiculo.setCellValueFactory(c -> new SimpleStringProperty(vehicleLabel(c.getValue())));
        colCita.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCitaId() == null ? "" : String.valueOf(c.getValue().getCitaId())));

        colDescripcion.setCellValueFactory(c -> {
            String desc = c.getValue().getDescripcion();
            if (desc == null) {
                return new SimpleStringProperty("");
            }
            String trimmed = desc.length() > 40 ? desc.substring(0, 37) + "..." : desc;
            return new SimpleStringProperty(trimmed);
        });
        colDescripcion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    Repair repair = (getTableRow() != null) ? getTableRow().getItem() : null;
                    if (repair != null && repair.getDescripcion() != null) {
                        setTooltip(new Tooltip(repair.getDescripcion()));
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });

        colEstado.setCellValueFactory(c -> new SimpleStringProperty(mapStatusToLabel(c.getValue().getEstado())));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tfx-badge", "tfx-badge-success", "tfx-badge-warning", "tfx-badge-danger");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    getStyleClass().add("tfx-badge");
                    switch (item) {
                        case "Finalizada", "Facturada" ->
                            getStyleClass().add("tfx-badge-success");
                        case "En proceso" ->
                            getStyleClass().add("tfx-badge-warning");
                        case "Cancelada" ->
                            getStyleClass().add("tfx-badge-danger");
                        default -> {
                        }
                    }
                }
            }
        });

        colImporteEstimado.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getImporteEstimado())));
        colImporteFinal.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getImporteFinal())));

        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnEstado = new Button("Estado");
            private final HBox box = new HBox(8, btnEditar, btnEstado);

            {
                btnEditar.getStyleClass().add("tfx-icon-btn");
                btnEstado.getStyleClass().add("tfx-icon-btn-secondary");

                btnEditar.setOnAction(e -> {
                    Repair r = (getTableRow() != null) ? getTableRow().getItem() : null;
                    if (r != null) {
                        openReparacionForm(r);
                    }
                });

                btnEstado.setOnAction(e -> {
                    Repair r = (getTableRow() != null) ? getTableRow().getItem() : null;
                    if (r != null) {
                        cambiarEstado(r);
                    }
                });
            }

            @Override
            protected void updateItem(Repair repair, boolean empty) {
                super.updateItem(repair, empty);
                setGraphic(empty || repair == null ? null : box);
            }
        });
        colAcciones.setSortable(false);

        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(10, 25, 50, 100));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(25));
        }

        smartTable = new SmartTable<>(tblReparaciones, masterData, null, cmbPageSize, lblResumen, "reparaciones", null);
        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblReparaciones.getFixedCellSize() + 2;
            tblReparaciones.setPrefHeight(tableHeight);
            tblReparaciones.setMinHeight(Region.USE_PREF_SIZE);
            tblReparaciones.setMaxHeight(Region.USE_PREF_SIZE);
        });

        smartTable.addColumnFilter(filterClienteField, (r, text) -> safe(r.getClienteNombre()).contains(text));
        smartTable.addColumnFilter(filterVehiculoField, (r, text) -> safe(vehicleLabel(r)).contains(text));
        smartTable.addColumnFilter(filterCitaField, (r, text) -> safe(r.getCitaId() == null ? "" : String.valueOf(r.getCitaId())).contains(text));
        smartTable.addColumnFilter(filterDescripcionField, (r, text) -> safe(r.getDescripcion()).contains(text));

        if (filterEstadoCombo != null) {
            filterEstadoCombo.getItems().clear();
            filterEstadoCombo.getItems().add("Todos");
            for (RepairStatus status : RepairStatus.values()) {
                filterEstadoCombo.getItems().add(mapStatusToLabel(status));
            }
            filterEstadoCombo.getSelectionModel().selectFirst();

            smartTable.addColumnFilter(filterEstadoCombo, (r, selected) -> {
                if (selected == null || "Todos".equalsIgnoreCase(selected)) {
                    return true;
                }
                return mapStatusToLabel(r.getEstado()).equalsIgnoreCase(selected);
            });
        }

        if (isSuperAdmin && filterEmpresaCombo != null) {
            smartTable.addColumnFilter(filterEmpresaCombo, (r, selected) -> {
                if (selected == null || "Todas".equalsIgnoreCase(selected)) {
                    return true;
                }
                return safeRaw(r.getEmpresaNombre()).equalsIgnoreCase(selected);
            });
        }

        setupRowDoubleClick();
        loadRepairsFromDb();
    }

    @FXML
    private void onNuevaReparacion() {
        openReparacionForm(null);
    }

    @FXML
    private void onRefrescar() {
        loadRepairsFromDb();
    }

    @FXML
    private void onLimpiarFiltros() {
        if (filterClienteField != null) {
            filterClienteField.clear();
        }
        if (filterVehiculoField != null) {
            filterVehiculoField.clear();
        }
        if (filterCitaField != null) {
            filterCitaField.clear();
        }
        if (filterDescripcionField != null) {
            filterDescripcionField.clear();
        }
        if (filterEstadoCombo != null) {
            filterEstadoCombo.getSelectionModel().selectFirst();
        }
        if (AuthContext.isSuperAdmin() && filterEmpresaCombo != null) {
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }
        if (smartTable != null) {
            smartTable.refresh();
        }
    }

    private void loadRepairsFromDb() {
        List<Repair> repairs = listRepairsUseCase.execute();
        repairs.sort(Comparator.comparing(r -> Optional.ofNullable(r.getCreatedAt()).orElse(java.time.LocalDateTime.MIN), Comparator.reverseOrder()));
        masterData.setAll(repairs);

        if (AuthContext.isSuperAdmin() && filterEmpresaCombo != null) {
            var empresas = repairs.stream().map(Repair::getEmpresaNombre).filter(s -> s != null && !s.isBlank()).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            filterEmpresaCombo.setItems(FXCollections.observableArrayList(empresas));
            filterEmpresaCombo.getItems().add(0, "Todas");
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }

        smartTable.refresh();

        if (lblHeaderInfo != null) {
            lblHeaderInfo.setText(masterData.size() + " reparaciones registradas");
        }
    }

    private void setupRowDoubleClick() {
        tblReparaciones.setRowFactory(tv -> {
            TableRow<Repair> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openReparacionForm(row.getItem());
                }
            });
            return row;
        });
    }

    private void openReparacionForm(Repair repair) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ReparacionFormView.fxml"));
            Parent root = loader.load();
            ReparacionFormController controller = loader.getController();

            Long empresaId = repair != null ? repair.getEmpresaId() : SessionManager.getInstance().getCurrentEmpresaId();
            controller.init(empresaId, saveRepairUseCase, repair);

            Stage stage = new Stage();
            stage.setTitle(repair == null ? "Nueva reparación" : "Editar reparación");
            stage.initOwner(tblReparaciones.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadRepairsFromDb();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario: " + ex.getMessage()).showAndWait();
        }
    }

    private void cambiarEstado(Repair repair) {
        if (repair == null) {
            return;
        }

        List<String> options = FXCollections.observableArrayList();
        for (RepairStatus status : RepairStatus.values()) {
            options.add(mapStatusToLabel(status));
        }

        String currentLabel = mapStatusToLabel(repair.getEstado());
        ChoiceDialog<String> dialog = new ChoiceDialog<>(currentLabel, options);
        dialog.setTitle("Cambiar estado");
        dialog.setHeaderText("Selecciona el nuevo estado de la reparación");
        dialog.setContentText("Estado:");

        dialog.showAndWait().ifPresent(selected -> {
            RepairStatus newStatus = mapLabelToStatus(selected);
            if (newStatus == null) {
                return;
            }
            try {
                long empresaId = repair.getEmpresaId();
                changeRepairStatusUseCase.execute(repair.getId(), empresaId, newStatus);
                loadRepairsFromDb();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "No se pudo actualizar el estado: " + ex.getMessage()).showAndWait();
            }
        });
    }

    private String mapStatusToLabel(RepairStatus status) {
        if (status == null) {
            return "Abierta";
        }
        return switch (status) {
            case ABIERTA ->
                "Abierta";
            case EN_PROCESO ->
                "En proceso";
            case FINALIZADA ->
                "Finalizada";
            case FACTURADA ->
                "Facturada";
            case CANCELADA ->
                "Cancelada";
        };
    }

    private RepairStatus mapLabelToStatus(String label) {
        if (label == null) {
            return null;
        }
        return switch (label) {
            case "Abierta" ->
                RepairStatus.ABIERTA;
            case "En proceso" ->
                RepairStatus.EN_PROCESO;
            case "Finalizada" ->
                RepairStatus.FINALIZADA;
            case "Facturada" ->
                RepairStatus.FACTURADA;
            case "Cancelada" ->
                RepairStatus.CANCELADA;
            default ->
                null;
        };
    }

    private String formatPrice(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return priceFormat.format(value);
    }

    private String vehicleLabel(Repair repair) {
        if (repair == null) {
            return "";
        }
        if (repair.getVehiculoEtiqueta() != null && !repair.getVehiculoEtiqueta().isBlank()) {
            return repair.getVehiculoEtiqueta();
        }
        if (repair.getVehiculoId() != null) {
            return "ID " + repair.getVehiculoId();
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String safeRaw(String value) {
        return value == null ? "" : value;
    }
}
