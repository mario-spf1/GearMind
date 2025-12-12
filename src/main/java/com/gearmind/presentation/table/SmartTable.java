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

    private final BiPredicate<T, String> globalMatcher; // puede ser null

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
        this.entityLabelPlural = (entityLabelPlural == null || entityLabelPlural.isBlank()) ? "elementos" : entityLabelPlural;
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
        if (field == null || matcher == null) {
            return;
        }

        ColumnFilter<T> cf = new ColumnFilter<>(field, matcher);
        columnFilters.add(cf);

        field.textProperty().addListener((obs, o, n) -> refresh());
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

       combo.valueProperty().addListener((obs, o, n) -> refresh());
   }

    /**
     * Reaplica todos los filtros y repone los datos en la tabla.
     */
    public void refresh() {
        if (masterData == null) {
            table.setItems(FXCollections.observableArrayList());
            lastVisibleCount = 0;
            lastTotalCount = 0;
            updateSummaryLabel();
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

        int limit = filtered.size();
        if (pageSizeCombo != null) {
            Integer value = pageSizeCombo.getValue();
            if (value != null && value > 0) {
                limit = Math.min(value, filtered.size());
            }
        }

        List<T> visible = filtered.subList(0, limit);
        lastVisibleCount = visible.size();
        table.setItems(FXCollections.observableArrayList(visible));
        updateSummaryLabel();

        if (afterRefreshCallback != null) {
            afterRefreshCallback.run();
        }
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

    private void updateSummaryLabel() {
        if (summaryLabel == null) return;

        if (lastTotalCount == 0) {
            summaryLabel.setText("No hay " + entityLabelPlural + " que mostrar.");
            return;
        }

        if (lastVisibleCount == lastTotalCount) {
            summaryLabel.setText("Mostrando " + lastTotalCount + " " + entityLabelPlural + ".");
        } else {
            summaryLabel.setText("Mostrando " + lastVisibleCount + " de " + lastTotalCount + " " + entityLabelPlural + ".");
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
