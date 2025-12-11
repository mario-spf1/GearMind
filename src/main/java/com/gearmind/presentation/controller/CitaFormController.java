package com.gearmind.presentation.controller;

import com.gearmind.application.appointment.SaveAppointmentRequest;
import com.gearmind.application.appointment.SaveAppointmentUseCase;
import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentOrigin;
import com.gearmind.domain.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CitaFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private TextField txtClienteId;
    @FXML
    private TextField txtVehiculoId;
    @FXML
    private TextField txtEmpleadoId;
    @FXML
    private DatePicker dpFecha;
    @FXML
    private TextField txtHora;
    @FXML
    private TextArea txtNotas;

    private Long empresaId;
    private SaveAppointmentUseCase saveAppointmentUseCase;
    private Appointment existingAppointment;
    private boolean saved = false;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public void init(Long empresaId, SaveAppointmentUseCase saveAppointmentUseCase, Appointment existingAppointment) {
        this.empresaId = empresaId;
        this.saveAppointmentUseCase = saveAppointmentUseCase;
        this.existingAppointment = existingAppointment;

        if (existingAppointment != null) {
            lblTitulo.setText("Editar cita");

            if (existingAppointment.getCustomerId() != null) {
                txtClienteId.setText(String.valueOf(existingAppointment.getCustomerId()));
            }
            if (existingAppointment.getVehicleId() != null) {
                txtVehiculoId.setText(String.valueOf(existingAppointment.getVehicleId()));
            }

            if (existingAppointment.getDateTime() != null) {
                LocalDateTime dt = existingAppointment.getDateTime();
                dpFecha.setValue(dt.toLocalDate());
                txtHora.setText(dt.toLocalTime().format(timeFormatter));
            }

            if (existingAppointment.getNotes() != null) {
                txtNotas.setText(existingAppointment.getNotes());
            }

            if (existingAppointment.getEmployeeId() != null) {
                txtEmpleadoId.setText(String.valueOf(existingAppointment.getEmployeeId()));
            } else {
                fillCurrentUserInEmpleadoField();
            }

        } else {
            lblTitulo.setText("Nueva cita");
            dpFecha.setValue(LocalDate.now());
            fillCurrentUserInEmpleadoField();
        }
    }

    private void fillCurrentUserInEmpleadoField() {
        if (AuthContext.isLoggedIn()) {
            User current = AuthContext.getCurrentUser();
            if (current != null) {
                txtEmpleadoId.setText(String.valueOf(current.getId()));
            }
        }
    }

    @FXML
    private void onCancelar() {
        close();
    }

    @FXML
    private void onGuardar() {
        try {
            SaveAppointmentRequest request = buildRequest();
            saveAppointmentUseCase.execute(request);
            saved = true;
            close();
        } catch (IllegalArgumentException | DateTimeParseException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error al guardar la cita: " + e.getMessage());
        }
    }

    private SaveAppointmentRequest buildRequest() {
        if (empresaId == null) {
            throw new IllegalArgumentException("No se ha establecido la empresa para la cita.");
        }

        String clienteStr = txtClienteId.getText();
        if (clienteStr == null || clienteStr.isBlank()) {
            throw new IllegalArgumentException("El ID de cliente es obligatorio.");
        }

        Long clienteId;
        try {
            clienteId = Long.parseLong(clienteStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El ID de cliente debe ser numérico.");
        }

        Long vehiculoId = null;
        String vehiculoStr = txtVehiculoId.getText();
        if (vehiculoStr != null && !vehiculoStr.isBlank()) {
            try {
                vehiculoId = Long.parseLong(vehiculoStr.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El ID de vehículo debe ser numérico.");
            }
        }

        LocalDate fecha = dpFecha.getValue();
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }

        String horaStr = txtHora.getText();
        if (horaStr == null || horaStr.isBlank()) {
            throw new IllegalArgumentException("La hora es obligatoria (formato HH:mm).");
        }

        LocalTime hora;
        try {
            hora = LocalTime.parse(horaStr.trim(), timeFormatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La hora debe tener formato HH:mm.");
        }

        LocalDateTime dateTime = LocalDateTime.of(fecha, hora);

        SaveAppointmentRequest request = new SaveAppointmentRequest();

        if (existingAppointment != null) {
            request.setId(existingAppointment.getId());
        }

        request.setEmpresaId(empresaId);
        request.setCustomerId(clienteId);
        request.setVehicleId(vehiculoId);
        request.setDateTime(dateTime);
        request.setNotes(txtNotas.getText());

        if (existingAppointment != null && existingAppointment.getOrigin() != null) {
            request.setOrigin(existingAppointment.getOrigin());
        } else {
            request.setOrigin(AppointmentOrigin.INTERNAL);
        }

        Long employeeId = null;

        if (existingAppointment != null && existingAppointment.getEmployeeId() != null) {
            employeeId = existingAppointment.getEmployeeId();
        } else if (AuthContext.isLoggedIn()) {
            User current = AuthContext.getCurrentUser();
            if (current != null) {
                employeeId = current.getId();
            }
        }

        request.setEmployeeId(employeeId);
        return request;
    }

    private void close() {
        Stage stage = (Stage) lblTitulo.getScene().getWindow();
        stage.close();
    }

    public boolean isSaved() {
        return saved;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
