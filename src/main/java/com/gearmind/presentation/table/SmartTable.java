package com.gearmind.presentation.table;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Helper genérico para tablas con: - búsqueda global - filtros por columna
 * (fila de filtros) - paginación real (tamaño de página + navegación).
 */
public class SmartTable<T> {

    private static final double HEADER_HEIGHT = 28d;

    private final TableView<T> table;
    private final ObservableList<T> masterData;

    private final TextField globalSearchField;
    private final ComboBox<Integer> pageSizeCombo;
    private final Label summaryLabel;
    private final String entityLabelPlural;

    private final BiPredicate<T, String> globalMatcher;

    private final List<ColumnFilter<T>> columnFilters = new ArrayList<>();

    private int lastVisibleCount;
    private int lastTotalCount;
    private int lastFromIndex;

    // Paginación
    private int currentPage;      // 0-based
    private int lastTotalPages = 1;
    private Button prevButton;
    private Button nextButton;
    private Label pageLabel;
    private HBox paginationBox;

    private Runnable afterRefreshCallback;

    public SmartTable(TableView<T> table, ObservableList<T> masterData, TextField globalSearchField, ComboBox<Integer> pageSizeCombo, Label summaryLabel, String entityLabelPlural, BiPredicate<T, String> globalMatcher) {

        this.table = table;
        this.masterData = masterData;
        this.globalSearchField = globalSearchField;
        this.pageSizeCombo = pageSizeCombo;
        this.summaryLabel = summaryLabel;
        this.entityLabelPlural = (entityLabelPlural == null || entityLabelPlural.isBlank()) ? "elementos" : entityLabelPlural;
        this.globalMatcher = globalMatcher;

        if (this.table != null) {
            this.table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }

        if (this.globalSearchField != null) {
            this.globalSearchField.textProperty().addListener((obs, o, n) -> resetToFirstPageAndRefresh());
        }

        if (this.pageSizeCombo != null) {
            this.pageSizeCombo.valueProperty().addListener((obs, o, n) -> resetToFirstPageAndRefresh());
        }

        buildPaginationControls();
    }

    /**
     * Registrar callback a ejecutar después de cada refresh (opcional).
     */
    public void setAfterRefreshCallback(Runnable callback) {
        this.afterRefreshCallback = callback;
    }

    /**
     * Registra un filtro por columna.
     * field: TextField del footer.
     * matcher: recibe (fila, textoFiltroMinusculas) y devuelve true si coincide.
     */
    public void addColumnFilter(TextField field, BiPredicate<T, String> matcher) {
        if (field == null || matcher == null) {
            return;
        }

        ColumnFilter<T> cf = new ColumnFilter<>(field, matcher);
        columnFilters.add(cf);

        field.textProperty().addListener((obs, o, n) -> resetToFirstPageAndRefresh());
    }

    /**
    * Registra un filtro por columna basado en ComboBox.
    * combo: ComboBox del footer (p.ej. "Todos/Activo/Inactivo").
    * matcher: recibe (fila, valorSeleccionadoMinusculas) y devuelve true si coincide.
     */
    public void addColumnFilter(ComboBox<String> combo, BiPredicate<T, String> matcher) {
        if (combo == null || matcher == null) {
            return;
        }

        ColumnFilter<T> cf = new ColumnFilter<>(combo, matcher);
        columnFilters.add(cf);

        combo.valueProperty().addListener((obs, o, n) -> resetToFirstPageAndRefresh());
    }

    /**
     * Reaplica todos los filtros y repone los datos en la tabla.
     */
    public void refresh() {
        if (masterData == null) {
            table.setItems(FXCollections.observableArrayList());
            lastVisibleCount = 0;
            lastTotalCount = 0;
            lastFromIndex = 0;
            currentPage = 0;
            lastTotalPages = 1;
            updatePaginationControls(0);
            updateSummaryLabel(0);
            return;
        }

        List<T> filtered = new ArrayList<>(masterData);

        if (globalSearchField != null && globalMatcher != null) {
            String text = globalSearchField.getText();
            if (text != null && !text.isBlank()) {
                String lower = text.toLowerCase(Locale.ROOT);
                filtered = filtered.stream().filter(item -> globalMatcher.test(item, lower)).collect(Collectors.toList());
            }
        }

        filtered = filtered.stream().filter(this::matchesAllColumnFilters).collect(Collectors.toList());
        lastTotalCount = filtered.size();

        int pageSize = resolvePageSize();

        List<T> visible;
        if (pageSize <= 0) {
            // "Todos": una sola página con todos los registros.
            currentPage = 0;
            lastTotalPages = 1;
            lastFromIndex = 0;
            visible = filtered;
        } else {
            lastTotalPages = Math.max(1, (int) Math.ceil((double) lastTotalCount / pageSize));
            if (currentPage > lastTotalPages - 1) {
                currentPage = lastTotalPages - 1;
            }
            if (currentPage < 0) {
                currentPage = 0;
            }
            int from = Math.min(currentPage * pageSize, lastTotalCount);
            int to = Math.min(from + pageSize, lastTotalCount);
            lastFromIndex = from;
            visible = filtered.subList(from, to);
        }

        lastVisibleCount = visible.size();
        table.setItems(FXCollections.observableArrayList(visible));
        adjustTableHeight();
        updatePaginationControls(pageSize);
        updateSummaryLabel(pageSize);

        if (afterRefreshCallback != null) {
            afterRefreshCallback.run();
        }
    }

    private int resolvePageSize() {
        if (pageSizeCombo == null) {
            return 0;
        }
        Integer value = pageSizeCombo.getValue();
        return (value != null && value > 0) ? value : 0;
    }

    /**
     * Ajusta la altura de la tabla al alto de su contenido, pero sin estirarla
     * (maxHeight = contenido) ni clavarla (minHeight = 0). Con VBox.vgrow="ALWAYS"
     * en el FXML, la tabla queda en min(contenido, espacio disponible): compacta
     * cuando hay pocas filas y con scroll interno cuando no caben.
     */
    private void adjustTableHeight() {
        if (table == null) {
            return;
        }
        double cellSize = table.getFixedCellSize();
        if (cellSize <= 0) {
            return; // sin celda de tamaño fijo no calculamos; se deja el vgrow tal cual
        }
        int rows = Math.max(lastVisibleCount, 1);
        double content = HEADER_HEIGHT + rows * cellSize + 2;
        table.setMinHeight(0);
        table.setPrefHeight(content);
        table.setMaxHeight(content);
    }

    private void resetToFirstPageAndRefresh() {
        currentPage = 0;
        refresh();
    }

    private void goToPage(int page) {
        currentPage = page;
        refresh();
    }

    private boolean matchesAllColumnFilters(T item) {
        for (ColumnFilter<T> cf : columnFilters) {

            if (cf.field != null) {
                String text = cf.field.getText();
                if (text != null && !text.isBlank()) {
                    String lower = text.toLowerCase(Locale.ROOT);
                    if (!cf.matcher.test(item, lower)) {
                        return false;
                    }
                }
                continue;
            }

            if (cf.combo != null) {
                String value = cf.combo.getValue();
                if (value != null && !value.isBlank()) {
                    String lower = value.toLowerCase(Locale.ROOT);
                    if (!"todos".equals(lower)) {
                        if (!cf.matcher.test(item, lower)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void buildPaginationControls() {
        if (summaryLabel == null || !(summaryLabel.getParent() instanceof Pane parent)) {
            return;
        }

        prevButton = new Button("‹ Anterior");
        prevButton.getStyleClass().add("tfx-btn-filter-clear");
        prevButton.setOnAction(e -> goToPage(currentPage - 1));

        nextButton = new Button("Siguiente ›");
        nextButton.getStyleClass().add("tfx-btn-filter-clear");
        nextButton.setOnAction(e -> goToPage(currentPage + 1));

        pageLabel = new Label();
        pageLabel.getStyleClass().add("tfx-muted");

        paginationBox = new HBox(8, prevButton, pageLabel, nextButton);
        paginationBox.setAlignment(Pos.CENTER_RIGHT);
        paginationBox.setVisible(false);
        paginationBox.setManaged(false);

        parent.getChildren().add(paginationBox);
    }

    private void updatePaginationControls(int pageSize) {
        if (paginationBox == null) {
            return;
        }

        boolean paged = pageSize > 0 && lastTotalCount > pageSize;
        paginationBox.setVisible(paged);
        paginationBox.setManaged(paged);
        if (!paged) {
            return;
        }

        pageLabel.setText("Página " + (currentPage + 1) + " de " + lastTotalPages);
        prevButton.setDisable(currentPage <= 0);
        nextButton.setDisable(currentPage >= lastTotalPages - 1);
    }

    private void updateSummaryLabel(int pageSize) {
        if (summaryLabel == null) {
            return;
        }

        if (lastTotalCount == 0) {
            summaryLabel.setText("No hay " + entityLabelPlural + " que mostrar.");
            return;
        }

        if (pageSize <= 0 || lastVisibleCount == lastTotalCount) {
            summaryLabel.setText("Mostrando " + lastTotalCount + " " + entityLabelPlural + ".");
        } else {
            int from = lastFromIndex + 1;
            int to = lastFromIndex + lastVisibleCount;
            summaryLabel.setText("Mostrando " + from + "–" + to + " de " + lastTotalCount + " " + entityLabelPlural + ".");
        }
    }

    public int getLastVisibleCount() {
        return lastVisibleCount;
    }

    public int getLastTotalCount() {
        return lastTotalCount;
    }

    private static class ColumnFilter<T> {
        final TextField field;
        final ComboBox<String> combo;
        final BiPredicate<T, String> matcher;

        ColumnFilter(TextField field, BiPredicate<T, String> matcher) {
            this.field = field;
            this.combo = null;
            this.matcher = matcher;
        }

        ColumnFilter(ComboBox<String> combo, BiPredicate<T, String> matcher) {
            this.field = null;
            this.combo = combo;
            this.matcher = matcher;
        }
    }
}
