package com.gearmind.presentation.controller;

import com.gearmind.application.common.SessionManager;
import com.gearmind.application.customer.ListCustomersUseCase;
import com.gearmind.domain.customer.Customer;
import com.gearmind.infrastructure.customer.MySqlCustomerRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.List;

public class ClientesController {

    @FXML
    private TableView<Customer> tblClientes;

    @FXML
    private TableColumn<Customer, String> colNombre;

    @FXML
    private TableColumn<Customer, String> colEmail;

    @FXML
    private TableColumn<Customer, String> colTelefono;

    private final ListCustomersUseCase listCustomersUseCase;

    public ClientesController() {
        this.listCustomersUseCase = new ListCustomersUseCase(
                new MySqlCustomerRepository()
        );
    }

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        loadClientes();
    }

    private void loadClientes() {
        long empresaId = SessionManager.getInstance().getCurrentEmpresaId();
        List<Customer> clientes = listCustomersUseCase.listByEmpresa(empresaId);
        tblClientes.setItems(FXCollections.observableArrayList(clientes));
    }

    @FXML
    private void onVolverHome() {
        try {
            Stage stage = (Stage) tblClientes.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/HomeView.fxml")
            );
            Parent root = loader.load();

            Scene scene = new Scene(
                    root,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight()
            );

            scene.getStylesheets().add(
                    getClass().getResource("/styles/theme.css").toExternalForm()
            );
            scene.getStylesheets().add(
                    getClass().getResource("/styles/components.css").toExternalForm()
            );

            stage.setTitle("GearMind — Inicio");
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
