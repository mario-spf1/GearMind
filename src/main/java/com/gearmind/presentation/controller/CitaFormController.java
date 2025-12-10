package com.gearmind.presentation.controller;

import com.gearmind.application.appointment.SaveAppointmentRequest;
import com.gearmind.application.appointment.SaveAppointmentUseCase;
import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentOrigin;
import com.gearmind.domain.appointment.AppointmentStatus;
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
    private DatePicker dpFecha;
    @FXML
    private TextField txtHora;
    @FXML
    private TextField txtClienteId;
    @FXML
    private TextField txtVehiculoId;
    @FXML
    private TextArea txtNotas;
    @FXML
    private Label lblOrigen;

    private SaveAppointmentUseCase saveAppointmentUseCase;
    private Long empresaId;
    private Appointment editingAppointment;
    private boolean saved = false;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public void init(Long empresaId, SaveAppointmentUseCase useCase, Appointment appointment) {
        this.empresaId = empresaId;
        this.saveAppointmentUseCase = useCase;
        this.editingAppointment = appointment;

        if (appointment != null) {
            lblTitulo.setText("Editar cita");
            if (appointment.getDateTime() != null) {
                LocalDateTime dt = appointment.getDateTime();
                dpFecha.setValue(dt.toLocalDate());
                txtHora.setText(dt.toLocalTime().format(timeFormatter));
            }
            if (appointment.getCustomerId() != null) {
                txtClienteId.setText(String.valueOf(appointment.getCustomerId()));
            }
            if (appointment.getVehicleId() != null) {
                txtVehiculoId.setText(String.valueOf(appointment.getVehicleId()));
            }
            txtNotas.setText(appointment.getNotes() != null ? appointment.getNotes() : "");
        } else {
            lblTitulo.setText("Nueva cita");
            dpFecha.setValue(LocalDate.now());
        }

        AppointmentOrigin origin = appointment != null && appointment.getOrigin() != null ? appointment.getOrigin() : AppointmentOrigin.INTERNAL;
        lblOrigen.setText(origin == AppointmentOrigin.INTERNAL ? "Interno" : "Telegram");
    }

    @FXML
    private void onGuardar() {
        try {
            LocalDate fecha = dpFecha.getValue();
            if (fecha == null) {
                showError("Debes seleccionar una fecha.");
                return;
            }

            String horaStr = txtHora.getText();
            if (horaStr == null || horaStr.isBlank()) {
                showError("Debes introducir una hora (formato HH:MM).");
                return;
            }

            LocalTime hora;
            try {
                hora = LocalTime.parse(horaStr.trim(), timeFormatter);
            } catch (DateTimeParseException e) {
                showError("Hora no válida. Usa el formato HH:MM, por ejemplo 09:30.");
                return;
            }

            LocalDateTime dateTime = LocalDateTime.of(fecha, hora);

            Long clienteId;
            try {
                clienteId = Long.parseLong(txtClienteId.getText().trim());
            } catch (NumberFormatException e) {
                showError("Cliente ID debe ser un número.");
                return;
            }

            Long vehiculoId = null;
            String vehiculoText = txtVehiculoId.getText();
            if (vehiculoText != null && !vehiculoText.isBlank()) {
                try {
                    vehiculoId = Long.parseLong(vehiculoText.trim());
                } catch (NumberFormatException e) {
                    showError("Vehículo ID debe ser un número (o dejar vacío).");
                    return;
                }
            }

            String notas = txtNotas.getText();
            AppointmentOrigin origin = editingAppointment != null && editingAppointment.getOrigin() != null ? editingAppointment.getOrigin() : AppointmentOrigin.INTERNAL;
            AppointmentStatus status;

            if (editingAppointment != null && editingAppointment.getStatus() != null) {
                status = editingAppointment.getStatus();
            } else {
                status = AppointmentStatus.CONFIRMED;
            }

            Long employeeId = null;

            if (AuthContext.isLoggedIn()) {
                User u = AuthContext.getCurrentUser();
                if (u != null) {
                    employeeId = u.getId();
                }
            }

            SaveAppointmentRequest req = new SaveAppointmentRequest();

            if (editingAppointment != null) {
                req.setId(editingAppointment.getId());
            }

            req.setEmpresaId(empresaId);
            req.setEmployeeId(employeeId);
            req.setCustomerId(clienteId);
            req.setVehicleId(vehiculoId);
            req.setDateTime(dateTime);
            req.setStatus(status);
            req.setOrigin(origin);
            req.setNotes(notas);
            saveAppointmentUseCase.execute(req);
            saved = true;
            closeStage();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error al guardar la cita: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        closeStage();
    }

    public boolean isSaved() {
        return saved;
    }

    private void closeStage() {
        Stage stage = (Stage) dpFecha.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
