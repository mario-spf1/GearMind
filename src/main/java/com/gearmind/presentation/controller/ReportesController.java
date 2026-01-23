package com.gearmind.presentation.controller;

import com.gearmind.application.common.AuthContext;
import com.gearmind.application.inventory.ListInventoryMovementsUseCase;
import com.gearmind.application.invoice.ListInvoicesUseCase;
import com.gearmind.application.product.ListProductsUseCase;
import com.gearmind.application.repair.ListRepairsUseCase;
import com.gearmind.domain.inventory.InventoryMovement;
import com.gearmind.domain.inventory.InventoryMovementType;
import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceStatus;
import com.gearmind.domain.product.Product;
import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.repair.RepairStatus;
import com.gearmind.infrastructure.inventory.MySqlInventoryMovementRepository;
import com.gearmind.infrastructure.invoice.MySqlInvoiceRepository;
import com.gearmind.infrastructure.product.MySqlProductRepository;
import com.gearmind.infrastructure.repair.MySqlRepairRepository;
import com.gearmind.infrastructure.report.ReportPdfGenerator;
import com.gearmind.presentation.report.ReportItem;
import com.gearmind.presentation.table.SmartTable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ReportesController {

    @FXML
    private ComboBox<String> cmbTipo;
    @FXML
    private DatePicker dpDesde;
    @FXML
    private DatePicker dpHasta;
    @FXML
    private TextField txtFiltro;
    @FXML
    private ComboBox<Integer> cmbPageSize;
    @FXML
    private TableView<ReportItem> tblReportes;
    @FXML
    private TableColumn<ReportItem, String> colEmpresa;
    @FXML
    private TableColumn<ReportItem, String> colTipo;
    @FXML
    private TableColumn<ReportItem, String> colDescripcion;
    @FXML
    private TableColumn<ReportItem, String> colFecha;
    @FXML
    private TableColumn<ReportItem, String> colEstado;
    @FXML
    private TableColumn<ReportItem, String> colCantidad;
    @FXML
    private TableColumn<ReportItem, String> colTotal;
    @FXML
    private Label lblResumen;
    @FXML
    private Label lblHeaderInfo;

    private final ObservableList<ReportItem> masterData = FXCollections.observableArrayList();
    private SmartTable<ReportItem> smartTable;

    private final ListRepairsUseCase listRepairsUseCase;
    private final ListInvoicesUseCase listInvoicesUseCase;
    private final ListProductsUseCase listProductsUseCase;
    private final ListInventoryMovementsUseCase listInventoryMovementsUseCase;
    private final ReportPdfGenerator reportPdfGenerator = new ReportPdfGenerator();

    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.getDefault()));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReportesController() {
        this.listRepairsUseCase = new ListRepairsUseCase(new MySqlRepairRepository());
        this.listInvoicesUseCase = new ListInvoicesUseCase(new MySqlInvoiceRepository());
        this.listProductsUseCase = new ListProductsUseCase(new MySqlProductRepository());
        this.listInventoryMovementsUseCase = new ListInventoryMovementsUseCase(new MySqlInventoryMovementRepository());
    }

    @FXML
    private void initialize() {
        tblReportes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblReportes.setPlaceholder(new Label("No hay datos para el reporte."));

        colEmpresa.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getEmpresa())));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getTipo())));
        colDescripcion.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getDescripcion())));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getFechaLabel())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getEstado())));
        colCantidad.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getCantidadLabel())));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(formatTotal(c.getValue().getTotal())));

        if (!AuthContext.isSuperAdmin()) {
            colEmpresa.setVisible(false);
        }

        cmbTipo.setItems(FXCollections.observableArrayList("Reparaciones", "Ingresos", "Stock", "Movimientos"));
        cmbTipo.getSelectionModel().selectFirst();
        if (cmbPageSize != null) {
            cmbPageSize.setItems(FXCollections.observableArrayList(10, 25, 50, 100, 0));
            cmbPageSize.getSelectionModel().select(Integer.valueOf(25));

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
                        return 25;
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

        smartTable = new SmartTable<>(tblReportes, masterData, txtFiltro, cmbPageSize, lblResumen, "reportes", (item, text) -> matchesSearch(item, text));
        tblReportes.setFixedCellSize(28);

        smartTable.setAfterRefreshCallback(() -> {
            int rows = Math.max(smartTable.getLastVisibleCount(), 1);
            double headerHeight = 28;
            double tableHeight = headerHeight + rows * tblReportes.getFixedCellSize() + 2;

            tblReportes.setPrefHeight(tableHeight);
            tblReportes.setMinHeight(Region.USE_PREF_SIZE);
            tblReportes.setMaxHeight(Region.USE_PREF_SIZE);
        });
        onGenerar();
    }
    
    @FXML
    private void onRefrescar() {
        onGenerar();
    }

    @FXML
    private void onGenerar() {
        String tipo = cmbTipo != null ? cmbTipo.getValue() : "Reparaciones";
        List<ReportItem> items = switch (tipo) {
            case "Ingresos" ->
                buildIngresos();
            case "Stock" ->
                buildStock();
            case "Movimientos" ->
                buildMovimientos();
            default ->
                buildReparaciones();
        };

        items = applyDateRange(items, dpDesde != null ? dpDesde.getValue() : null, dpHasta != null ? dpHasta.getValue() : null);
        masterData.setAll(items);
        smartTable.refresh();

        if (lblHeaderInfo != null) {
            lblHeaderInfo.setText(masterData.size() + " registros en reporte");
        }
    }

    @FXML
    private void onExportarPdf() {
        if (masterData.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No hay datos para exportar.").showAndWait();
            return;
        }

        String tipo = cmbTipo != null ? cmbTipo.getValue() : "Reporte";
        String summary = buildFilterSummary();
        try {
            var path = reportPdfGenerator.generate("Reporte " + tipo, tblReportes.getItems(), summary);
            new Alert(Alert.AlertType.INFORMATION, "Reporte exportado en:\n" + path).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "No se pudo exportar el reporte: " + ex.getMessage()).showAndWait();
        }
    }

    private List<ReportItem> buildReparaciones() {
        List<Repair> repairs = listRepairsUseCase.execute();
        return repairs.stream()
                .sorted(Comparator.comparing(r -> Optional.ofNullable(r.getCreatedAt()).orElse(LocalDateTime.MIN), Comparator.reverseOrder()))
                .map(r -> new ReportItem(
                AuthContext.isSuperAdmin() ? safe(r.getEmpresaNombre()) : safe(AuthContext.getEmpresaNombre()),
                "Reparación",
                buildRepairDescription(r),
                mapRepairStatus(r.getEstado()),
                r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate() : null,
                formatDate(r.getCreatedAt()),
                "—",
                Optional.ofNullable(r.getImporteFinal()).orElse(r.getImporteEstimado())
        ))
                .toList();
    }

    private List<ReportItem> buildIngresos() {
        List<Invoice> invoices = AuthContext.isSuperAdmin()
                ? listInvoicesUseCase.listAllWithEmpresa()
                : listInvoicesUseCase.listByEmpresa(AuthContext.getEmpresaId());

        return invoices.stream()
                .sorted(Comparator.comparing(i -> Optional.ofNullable(i.getFecha()).orElse(LocalDateTime.MIN), Comparator.reverseOrder()))
                .map(i -> new ReportItem(
                AuthContext.isSuperAdmin() ? safe(i.getEmpresaNombre()) : safe(AuthContext.getEmpresaNombre()),
                "Factura",
                buildInvoiceDescription(i),
                mapInvoiceStatus(i.getEstado()),
                i.getFecha() != null ? i.getFecha().toLocalDate() : null,
                formatDate(i.getFecha()),
                "—",
                i.getTotal()
        ))
                .toList();
    }

    private List<ReportItem> buildStock() {
        List<Product> products = AuthContext.isSuperAdmin()
                ? listProductsUseCase.listAllWithEmpresa()
                : listProductsUseCase.listByEmpresa(AuthContext.getEmpresaId());

        return products.stream()
                .sorted(Comparator.comparing(Product::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(p -> new ReportItem(
                AuthContext.isSuperAdmin() ? safe(p.getEmpresaNombre()) : safe(AuthContext.getEmpresaNombre()),
                "Stock",
                buildProductDescription(p),
                p.isActivo() ? "Activo" : "Inactivo",
                p.getUpdatedAt() != null ? p.getUpdatedAt().toLocalDate() : null,
                formatDate(p.getUpdatedAt()),
                p.getStock() == null ? "0" : String.valueOf(p.getStock()),
                p.getPrecioVenta()
        ))
                .toList();
    }

    private List<ReportItem> buildMovimientos() {
        List<InventoryMovement> movements = AuthContext.isSuperAdmin() ? listInventoryMovementsUseCase.listAllWithEmpresa() : listInventoryMovementsUseCase.listByEmpresa(AuthContext.getEmpresaId());

        return movements.stream().sorted(Comparator.comparing(m -> Optional.ofNullable(m.getCreatedAt()).orElse(LocalDateTime.MIN), Comparator.reverseOrder()))
                .map(m -> new ReportItem(
                AuthContext.isSuperAdmin() ? safe(m.getEmpresaNombre()) : safe(AuthContext.getEmpresaNombre()),
                "Movimiento",
                buildMovementDescription(m),
                mapMovementStatus(m.getTipo()),
                m.getCreatedAt() != null ? m.getCreatedAt().toLocalDate() : null,
                formatDate(m.getCreatedAt()),
                m.getCantidad() == null ? "0" : String.valueOf(m.getCantidad()),
                null))
                .toList();
    }

    private List<ReportItem> applyDateRange(List<ReportItem> items, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return items;
        }
        return items.stream()
                .filter(item -> {
                    LocalDate date = item.getFecha();
                    if (date == null) {
                        return false;
                    }
                    boolean afterFrom = from == null || !date.isBefore(from);
                    boolean beforeTo = to == null || !date.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .toList();
    }

    private String buildRepairDescription(Repair repair) {
        String cliente = safe(repair.getClienteNombre());
        String vehiculo = safe(repair.getVehiculoEtiqueta());
        String base = "Reparación #" + repair.getId();
        if (!cliente.isBlank() || !vehiculo.isBlank()) {
            return base + " · " + cliente + " · " + vehiculo;
        }
        return base;
    }

    private String buildInvoiceDescription(Invoice invoice) {
        String cliente = safe(invoice.getClienteNombre());
        String numero = safe(invoice.getNumero());
        String base = "Factura" + (numero.isBlank() ? " #" + invoice.getId() : " " + numero);
        if (!cliente.isBlank()) {
            return base + " · " + cliente;
        }
        return base;
    }

    private String buildProductDescription(Product product) {
        String referencia = safe(product.getReferencia());
        String nombre = safe(product.getNombre());
        String base = nombre.isBlank() ? "Producto" : nombre;
        if (!referencia.isBlank()) {
            return base + " · Ref " + referencia;
        }
        return base;
    }

    private String buildMovementDescription(InventoryMovement movement) {
        String nombre = safe(movement.getProductoNombre());
        String referencia = safe(movement.getProductoReferencia());
        String base = nombre.isBlank() ? "Producto" : nombre;
        if (!referencia.isBlank()) {
            base += " · Ref " + referencia;
        }
        if (movement.getReferencia() != null && !movement.getReferencia().isBlank()) {
            base += " · " + movement.getReferencia();
        }
        return base;
    }

    private String mapMovementStatus(InventoryMovementType tipo) {
        if (tipo == null) {
            return "Salida";
        }
        return tipo == InventoryMovementType.ENTRADA ? "Entrada" : "Salida";
    }

    private String mapRepairStatus(RepairStatus status) {
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

    private String mapInvoiceStatus(InvoiceStatus status) {
        if (status == null) {
            return "Pendiente";
        }
        return switch (status) {
            case BORRADOR ->
                "Borrador";
            case PENDIENTE ->
                "Pendiente";
            case PAGADA ->
                "Pagada";
            case ANULADA ->
                "Anulada";
        };
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateFormatter.format(dateTime);
    }

    private String formatTotal(BigDecimal total) {
        if (total == null) {
            return "";
        }
        return priceFormat.format(total);
    }

    private boolean matchesSearch(ReportItem item, String text) {
        String needle = text.toLowerCase(Locale.ROOT);
        return safeLower(item.getTipo()).contains(needle)
                || safeLower(item.getDescripcion()).contains(needle)
                || safeLower(item.getEstado()).contains(needle)
                || safeLower(item.getEmpresa()).contains(needle);
    }

    private String buildFilterSummary() {
        String tipo = cmbTipo != null ? cmbTipo.getValue() : "Reporte";
        StringBuilder sb = new StringBuilder("Tipo: ").append(tipo);
        if (dpDesde != null && dpDesde.getValue() != null) {
            sb.append(" · Desde: ").append(dpDesde.getValue().format(dateFormatter));
        }
        if (dpHasta != null && dpHasta.getValue() != null) {
            sb.append(" · Hasta: ").append(dpHasta.getValue().format(dateFormatter));
        }
        if (txtFiltro != null && txtFiltro.getText() != null && !txtFiltro.getText().isBlank()) {
            sb.append(" · Búsqueda: ").append(txtFiltro.getText().trim());
        }
        return sb.toString();
    }

    private String safe(String text) {
        if (text == null) {
            return "";
        }
        return text;
    }

    private String safeLower(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT);
    }
}
