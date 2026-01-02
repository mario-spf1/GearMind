package com.gearmind.presentation.controller;

import com.gearmind.application.budget.ListBudgetsUseCase;
import com.gearmind.application.common.AuthContext;
import com.gearmind.application.common.SessionManager;
import com.gearmind.domain.budget.Budget;
import com.gearmind.domain.budget.BudgetStatus;
import com.gearmind.infrastructure.budget.BudgetPdfStorage;
import com.gearmind.infrastructure.budget.MySqlBudgetRepository;
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

import java.awt.Desktop;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PresupuestosController {

    @FXML
    private TableView<Budget> tblPresupuestos;
    @FXML
    private TableColumn<Budget, String> colEmpresa;
    @FXML
    private TableColumn<Budget, String> colCliente;
    @FXML
    private TableColumn<Budget, String> colVehiculo;
    @FXML
    private TableColumn<Budget, String> colFecha;
    @FXML
    private TableColumn<Budget, String> colEstado;
    @FXML
    private TableColumn<Budget, String> colTotal;
    @FXML
    private TableColumn<Budget, Budget> colAcciones;

    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private Button btnNuevoPresupuesto;
    @FXML
    private Label lblHeaderInfo;
    @FXML
    private Label lblResumen;

    @FXML
    private TextField filterClienteField;
    @FXML
    private TextField filterVehiculoField;
    @FXML
    private ComboBox<String> filterEstadoCombo;
    @FXML
    private ComboBox<String> filterEmpresaCombo;
    @FXML
    private HBox boxFilterEmpresa;

    private final ObservableList<Budget> masterData = FXCollections.observableArrayList();
    private SmartTable<Budget> smartTable;

    private final ListBudgetsUseCase listBudgetsUseCase;
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.getDefault()));

    public PresupuestosController() {
        this.listBudgetsUseCase = new ListBudgetsUseCase(new MySqlBudgetRepository());
    }

    @FXML
    private void initialize() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();

        tblPresupuestos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblPresupuestos.setPlaceholder(new Label("No hay presupuestos que mostrar."));

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
        colVehiculo.setCellValueFactory(c -> new SimpleStringProperty(safeRaw(c.getValue().getVehiculoEtiqueta())));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getFecha())));
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
                        case "Aceptado" ->
                            getStyleClass().add("tfx-badge-success");
                        case "Enviado" ->
                            getStyleClass().add("tfx-badge-warning");
                        case "Rechazado" ->
                            getStyleClass().add("tfx-badge-danger");
                        default -> {
                        }
                    }
                }
            }
        });
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(c.getValue().getTotalEstimado())));

        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnPdf = new Button("PDF");
            private final HBox box = new HBox(8, btnEditar, btnPdf);

            {
                btnEditar.getStyleClass().add("tfx-icon-btn");
                btnPdf.getStyleClass().add("tfx-icon-btn-secondary");

                btnEditar.setOnAction(e -> {
                    Budget budget = getItem();
                    if (budget != null) {
                        openPresupuestoForm(budget);
                    }
                });

                btnPdf.setOnAction(e -> {
                    Budget budget = getItem();
                    if (budget != null) {
                        openPdf(budget.getId());
                    }
                });
            }

            @Override
            protected void updateItem(Budget budget, boolean empty) {
                super.updateItem(budget, empty);
                setGraphic(empty || budget == null ? null : box);
            }
        });
        colAcciones.setSortable(false);

        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(10, 25, 50, 100));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(25));
        }

        smartTable = new SmartTable<>(tblPresupuestos, masterData, null, cmbPageSize, lblResumen, "presupuestos", null);

        tblPresupuestos.setFixedCellSize(28);
        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblPresupuestos.getFixedCellSize() + 2;
            tblPresupuestos.setPrefHeight(tableHeight);
            tblPresupuestos.setMinHeight(Region.USE_PREF_SIZE);
        });

        smartTable.addColumnFilter(filterClienteField, (b, text) -> safe(b.getClienteNombre()).contains(text));
        smartTable.addColumnFilter(filterVehiculoField, (b, text) -> safe(b.getVehiculoEtiqueta()).contains(text));

        if (filterEstadoCombo != null) {
            filterEstadoCombo.setItems(FXCollections.observableArrayList("Todos", "Borrador", "Enviado", "Aceptado", "Rechazado"));
            filterEstadoCombo.getSelectionModel().select("Todos");

            smartTable.addColumnFilter(filterEstadoCombo, (b, selected) -> {
                if (selected == null || "Todos".equalsIgnoreCase(selected)) {
                    return true;
                }
                return mapStatusToLabel(b.getEstado()).equalsIgnoreCase(selected);
            });
        }

        if (isSuperAdmin && filterEmpresaCombo != null) {
            smartTable.addColumnFilter(filterEmpresaCombo, (b, selected) -> {
                if (selected == null || "Todas".equalsIgnoreCase(selected)) {
                    return true;
                }
                return safeRaw(b.getEmpresaNombre()).equalsIgnoreCase(selected);
            });
        }

        setupRowDoubleClick();
        loadBudgetsFromDb();
    }

    @FXML
    private void onRefrescar() {
        loadBudgetsFromDb();
    }

    @FXML
    private void onNuevoPresupuesto() {
        openPresupuestoForm(null);
    }

    @FXML
    private void onLimpiarFiltros() {
        if (filterClienteField != null) {
            filterClienteField.clear();
        }
        if (filterVehiculoField != null) {
            filterVehiculoField.clear();
        }
        if (filterEstadoCombo != null) {
            filterEstadoCombo.getSelectionModel().select("Todos");
        }
        if (AuthContext.isSuperAdmin() && filterEmpresaCombo != null) {
            filterEmpresaCombo.getSelectionModel().select("Todas");
        }
    }

    private void loadBudgetsFromDb() {
        boolean isSuperAdmin = AuthContext.isSuperAdmin();
        List<Budget> budgets;

        if (isSuperAdmin) {
            budgets = listBudgetsUseCase.listAllWithEmpresa();
        } else {
            long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
            budgets = listBudgetsUseCase.listByEmpresa(empresaId);
        }

        budgets.sort(Comparator.comparing(Budget::getFecha, Comparator.nullsLast(Comparator.reverseOrder())));
        masterData.setAll(budgets);

        if (isSuperAdmin && filterEmpresaCombo != null) {
            var empresas = budgets.stream()
                    .map(Budget::getEmpresaNombre)
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
            lblHeaderInfo.setText(masterData.size() + " presupuestos registrados");
        }
    }

    private void setupRowDoubleClick() {
        tblPresupuestos.setRowFactory(tv -> {
            TableRow<Budget> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openPresupuestoForm(row.getItem());
                }
            });
            return row;
        });
    }

    private void openPresupuestoForm(Budget budget) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/PresupuestoFormView.fxml"));
            Parent root = loader.load();
            PresupuestoFormController controller = loader.getController();
            if (budget == null) {
                controller.initForNew();
            } else {
                controller.initForEdit(budget.getId());
            }

            Stage stage = new Stage();
            stage.setTitle(budget == null ? "Nuevo presupuesto" : "Editar presupuesto");
            stage.initOwner(tblPresupuestos.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setResizable(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            if (controller.isSaved()) {
                loadBudgetsFromDb();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario de presupuesto: " + ex.getMessage()).showAndWait();
        }
    }

    private void openPdf(long budgetId) {
        Path path = BudgetPdfStorage.resolvePath(budgetId);
        if (!Files.exists(path)) {
            new Alert(Alert.AlertType.WARNING, "No se encontró el PDF. Genera el presupuesto para crear el archivo.").showAndWait();
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(path.toFile());
            } else {
                new Alert(Alert.AlertType.WARNING, "No se pudo abrir el PDF automáticamente. Ruta: " + path).showAndWait();
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el PDF: " + ex.getMessage()).showAndWait();
        }
    }

    private String formatPrice(BigDecimal value) {
        if (value == null) {
            return "0,00";
        }
        return priceFormat.format(value);
    }

    private String formatDate(java.time.LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String mapStatusToLabel(BudgetStatus status) {
        if (status == null) {
            return "Borrador";
        }
        return switch (status) {
            case BORRADOR ->
                "Borrador";
            case ENVIADO ->
                "Enviado";
            case ACEPTADO ->
                "Aceptado";
            case RECHAZADO ->
                "Rechazado";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.getDefault());
    }

    private String safeRaw(String value) {
        return value == null ? "" : value;
    }
}
