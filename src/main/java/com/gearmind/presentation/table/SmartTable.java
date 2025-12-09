package com.gearmind.presentation.table;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Helper genérico para tablas con:
 *  - búsqueda global
 *  - filtros por columna (fila de filtros)
 *  - límite de filas (page size)
 *  - resumen "Mostrando X de Y ..."
 */
public class SmartTable<T> {

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

    private Runnable afterRefreshCallback;

    public SmartTable(TableView<T> table, ObservableList<T> masterData, TextField globalSearchField, ComboBox<Integer> pageSizeCombo, Label summaryLabel, String entityLabelPlural, BiPredicate<T, String> globalMatcher) {

        this.table = table;
        this.masterData = masterData;
        this.globalSearchField = globalSearchField;
        this.pageSizeCombo = pageSizeCombo;
        this.summaryLabel = summaryLabel;
        this.entityLabelPlural = entityLabelPlural;
        this.globalMatcher = globalMatcher;

        if (this.globalSearchField != null) {
            this.globalSearchField.textProperty().addListener((obs, o, n) -> refresh());
        }

        if (this.pageSizeCombo != null) {
            this.pageSizeCombo.valueProperty().addListener((obs, o, n) -> refresh());
        }
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
        if (field == null || matcher == null) return;

        ColumnFilter<T> cf = new ColumnFilter<>(field, matcher);
        columnFilters.add(cf);

        field.textProperty().addListener((obs, o, n) -> refresh());
    }

    /**
     * Debe llamarse tras modificar masterData (ej. al cargar de BD).
     */
    public void refresh() {
        // Texto de búsqueda global
        String globalText = "";
        if (globalSearchField != null && globalSearchField.getText() != null) {
            globalText = globalSearchField.getText().trim().toLowerCase(Locale.ROOT);
        }
        final String search = globalText;

        int limit = Integer.MAX_VALUE;
        if (pageSizeCombo != null && pageSizeCombo.getValue() != null) {
            limit = pageSizeCombo.getValue();
        }

        List<T> filtered = masterData.stream().filter(item -> {
            if (globalMatcher == null || search.isBlank()) {
                return true;
            }
            return globalMatcher.test(item, search);
        }).filter(this::matchesAllColumnFilters).collect(Collectors.toList());

        lastTotalCount = filtered.size();

        List<T> visible = filtered.subList(0, Math.min(limit, filtered.size()));
        lastVisibleCount = visible.size();
        table.setItems(FXCollections.observableArrayList(visible));
        updateSummaryLabel();

        if (afterRefreshCallback != null) {
            afterRefreshCallback.run();
        }
    }

    private boolean matchesAllColumnFilters(T item) {
        for (ColumnFilter<T> cf : columnFilters) {
            String text = cf.field.getText();
            if (text != null && !text.isBlank()) {
                String lower = text.toLowerCase(Locale.ROOT);
                if (!cf.matcher.test(item, lower)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void updateSummaryLabel() {
        if (summaryLabel == null) return;

        if (lastTotalCount == 0) {
            summaryLabel.setText("No hay " + entityLabelPlural + " que mostrar.");
        } else {
            summaryLabel.setText("Mostrando " + lastVisibleCount + " de " + lastTotalCount + " " + entityLabelPlural);
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
        final BiPredicate<T, String> matcher;

        ColumnFilter(TextField field, BiPredicate<T, String> matcher) {
            this.field = field;
            this.matcher = matcher;
        }
    }
}
