package com.gearmind.presentation.controller;

import com.gearmind.domain.repair.Repair;
import com.gearmind.infrastructure.document.RepairDocumentStorage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class DocumentosReparacionController {

    @FXML
    private Label lblTitulo;
    @FXML
    private ListView<DocumentItem> lstDocumentos;
    @FXML
    private Button btnAbrir;

    private final ObservableList<DocumentItem> documentos = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private long repairId;

    @FXML
    private void initialize() {
        lstDocumentos.setItems(documentos);
        lstDocumentos.setPlaceholder(new Label("No hay documentos asociados."));
        lstDocumentos.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DocumentItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.nombre + " · " + item.sizeLabel + " · " + item.updatedLabel);
                }
            }
        });

        lstDocumentos.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                abrirSeleccionado();
            }
        });
    }

    public void init(Repair repair) {
        if (repair == null || repair.getId() == null) {
            throw new IllegalArgumentException("La reparación es obligatoria");
        }
        this.repairId = repair.getId();
        if (lblTitulo != null) {
            lblTitulo.setText("Documentos de reparación #" + repairId);
        }
        loadDocuments();
    }

    @FXML
    private void onAgregarDocumento() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar documento");
        File file = chooser.showOpenDialog(currentStage());
        if (file == null) {
            return;
        }

        try {
            Path targetDir = RepairDocumentStorage.resolveDir(repairId);
            Files.createDirectories(targetDir);
            Path target = uniqueTargetPath(targetDir, file.getName());
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            loadDocuments();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar el documento: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onAbrirDocumento() {
        abrirSeleccionado();
    }

    @FXML
    private void onCerrar() {
        Stage stage = currentStage();
        if (stage != null) {
            stage.close();
        }
    }

    private void abrirSeleccionado() {
        DocumentItem selected = lstDocumentos.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Selecciona un documento para abrir.").showAndWait();
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            new Alert(Alert.AlertType.WARNING, "No se puede abrir el documento en este entorno.").showAndWait();
            return;
        }
        try {
            Desktop.getDesktop().open(selected.path.toFile());
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el documento: " + ex.getMessage()).showAndWait();
        }
    }

    private void loadDocuments() {
        Path dir = RepairDocumentStorage.resolveDir(repairId);
        if (!Files.exists(dir)) {
            documentos.clear();
            return;
        }
        try {
            List<DocumentItem> items = Files.list(dir)
                    .filter(Files::isRegularFile)
                    .map(this::toDocumentItem)
                    .sorted(Comparator.comparing((DocumentItem d) -> d.updated).reversed())
                    .toList();
            documentos.setAll(items);
        } catch (IOException ex) {
            documentos.clear();
        }
    }

    private DocumentItem toDocumentItem(Path path) {
        try {
            long size = Files.size(path);
            Instant updated = Files.getLastModifiedTime(path).toInstant();
            String sizeLabel = formatSize(size);
            String updatedLabel = dateFormatter.format(LocalDateTime.ofInstant(updated, ZoneId.systemDefault()));
            return new DocumentItem(path, path.getFileName().toString(), sizeLabel, updated, updatedLabel);
        } catch (IOException ex) {
            return new DocumentItem(path, path.getFileName().toString(), "—", Instant.EPOCH, "—");
        }
    }

    private Path uniqueTargetPath(Path dir, String fileName) {
        Path target = dir.resolve(fileName);
        if (!Files.exists(target)) {
            return target;
        }
        String base = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        }
        int i = 1;
        while (Files.exists(target)) {
            target = dir.resolve(base + " (" + i + ")" + ext);
            i++;
        }
        return target;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }

    private Stage currentStage() {
        return lstDocumentos != null ? (Stage) lstDocumentos.getScene().getWindow() : null;
    }

    private static class DocumentItem {

        private final Path path;
        private final String nombre;
        private final String sizeLabel;
        private final Instant updated;
        private final String updatedLabel;

        private DocumentItem(Path path, String nombre, String sizeLabel, Instant updated, String updatedLabel) {
            this.path = path;
            this.nombre = nombre;
            this.sizeLabel = sizeLabel;
            this.updated = updated;
            this.updatedLabel = updatedLabel;
        }
    }
}
