package com.gearmind.application.appointment;

import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentRepository;

import java.util.Optional;

public class GetAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;

    public GetAppointmentUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Appointment execute(Long appointmentId) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("El id de la cita es obligatorio.");
        }

        Optional<Appointment> optionalAppointment = appointmentRepository.findById(appointmentId);
        return optionalAppointment.orElseThrow(() -> new IllegalArgumentException("Cita no encontrada con id: " + appointmentId));
    }
}
