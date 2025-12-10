package com.gearmind.application.appointment;

import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentRepository;

import java.util.List;

public class ListAppointmentsUseCase {

    private final AppointmentRepository appointmentRepository;

    public ListAppointmentsUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public List<Appointment> execute(Long empresaId) {
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria para listar las citas.");
        }
        return appointmentRepository.findByEmpresa(empresaId);
    }
}
