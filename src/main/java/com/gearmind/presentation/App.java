package com.gearmind.presentation;

import com.gearmind.AppVersion;
import com.gearmind.infrastructure.database.DatabaseMigrator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            DatabaseMigrator.migrate();
        } catch (RuntimeException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("GearMind — Error de actualización");
            alert.setHeaderText("No se pudo actualizar la base de datos");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
            throw ex;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
        Scene scene = new Scene(loader.load(), 960, 600);

        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());

        stage.setTitle("GearMind v" + AppVersion.VERSION + " — Acceso");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
