package com.gearmind.launcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LauncherApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LauncherView.fxml"));
        Parent root = loader.load();
        LauncherController controller = loader.getController();

        Scene scene = new Scene(root, 520, 600);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());

        stage.setTitle("GearMind — Launcher");
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> controller.onClose());
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
