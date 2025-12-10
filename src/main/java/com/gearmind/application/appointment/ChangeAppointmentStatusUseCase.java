package com.gearmind.application.appointment;

import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentRepository;
import com.gearmind.domain.appointment.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public class ChangeAppointmentStatusUseCase {

    private final AppointmentRepository appointmentRepository;

    public ChangeAppointmentStatusUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
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
    }
}
