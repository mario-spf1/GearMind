package com.gearmind.application.appointment;

import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentRepository;
import com.gearmind.domain.appointment.AppointmentStatus;
import com.gearmind.application.telegram.SendTelegramNotificationUseCase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ChangeAppointmentStatusUseCase {

    private final AppointmentRepository appointmentRepository;
    private final SendTelegramNotificationUseCase notificationUseCase;

    public ChangeAppointmentStatusUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
        this.notificationUseCase = null;
    }

    public ChangeAppointmentStatusUseCase(AppointmentRepository appointmentRepository, SendTelegramNotificationUseCase notificationUseCase) {
        this.appointmentRepository = appointmentRepository;
        this.notificationUseCase = notificationUseCase;
    }

    public void execute(Long appointmentId, Long empresaId, AppointmentStatus newStatus) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("El id de la cita es obligatorio.");
        }
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio.");
        }

        Optional<Appointment> optionalAppointment = appointmentRepository.findById(appointmentId);
        Appointment appointment = optionalAppointment.orElseThrow(() -> new IllegalArgumentException("Cita no encontrada con id: " + appointmentId));

        if (!empresaId.equals(appointment.getEmpresaId())) {
            throw new IllegalArgumentException("La cita no pertenece a la empresa indicada.");
        }

        appointment.setStatus(newStatus);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
        if (notificationUseCase != null && appointment.getCustomerId() != null) {
            String statusLabel = mapStatusLabel(newStatus);
            String when = appointment.getDateTime() != null ? appointment.getDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "fecha por confirmar";
            String message = "Tu cita #" + appointment.getId() + " para el " + when + " ha cambiado a estado: " + statusLabel + ".";
            String payload = "appointmentId=" + appointment.getId() + ";status=" + newStatus.name();
            notificationUseCase.execute(appointment.getCustomerId(), "CITA_ESTADO", message, payload);
        }
    }

    private String mapStatusLabel(AppointmentStatus status) {
        return switch (status) {
            case REQUESTED ->
                "Solicitada";
            case CONFIRMED ->
                "Confirmada";
            case CANCELLED ->
                "Cancelada";
            case COMPLETED ->
                "Completada";
        };
    }
}
