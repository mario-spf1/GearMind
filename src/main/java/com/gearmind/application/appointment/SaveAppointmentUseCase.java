package com.gearmind.application.appointment;

import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentOrigin;
import com.gearmind.domain.appointment.AppointmentRepository;
import com.gearmind.domain.appointment.AppointmentStatus;
import com.gearmind.domain.appointment.OverlappingAppointmentException;
import java.time.LocalDateTime;
import java.util.Optional;

public class SaveAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;

    public SaveAppointmentUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public void execute(SaveAppointmentRequest request) {

        validateRequest(request);
        Long appointmentId = request.getId();
        LocalDateTime now = LocalDateTime.now();

        if (appointmentRepository.existsAtDateTime(request.getEmpresaId(), request.getVehicleId(), request.getDateTime(), appointmentId)) {
            throw new OverlappingAppointmentException("Ya existe otra cita en esa fecha y hora para ese vehículo.");
        }

        AppointmentOrigin origin = defaultOrigin(request.getOrigin());
        AppointmentStatus status = defaultStatus(request.getStatus(), origin);

        if (appointmentId == null) {
            Appointment appointment = new Appointment(null, request.getEmpresaId(), request.getEmployeeId(), request.getCustomerId(), request.getVehicleId(), request.getDateTime(), status, origin, request.getNotes(), now, null);
            appointmentRepository.save(appointment);
        } else {
            Optional<Appointment> optionalAppointment = appointmentRepository.findById(appointmentId);
            Appointment appointment = optionalAppointment.orElseThrow(() -> new IllegalArgumentException("Cita no encontrada con id: " + appointmentId));
            appointment.setCustomerId(request.getCustomerId());
            appointment.setVehicleId(request.getVehicleId());
            appointment.setDateTime(request.getDateTime());
            appointment.setNotes(request.getNotes());

            if (request.getOrigin() != null) {
                appointment.setOrigin(request.getOrigin());
            }

            if (request.getStatus() != null) {
                appointment.setStatus(request.getStatus());
            }

            if (request.getEmployeeId() != null) {
                appointment.setEmployeeId(request.getEmployeeId());
            }

            appointment.setUpdatedAt(now);
            appointmentRepository.save(appointment);
        }
    }

    private void validateRequest(SaveAppointmentRequest request) {
        if (request.getEmpresaId() == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        if (request.getDateTime() == null) {
            throw new IllegalArgumentException("La fecha y hora de la cita son obligatorias.");
        }
    }

    private AppointmentOrigin defaultOrigin(AppointmentOrigin origin) {
        return origin != null ? origin : AppointmentOrigin.INTERNAL;
    }

    private AppointmentStatus defaultStatus(AppointmentStatus statusFromRequest, AppointmentOrigin origin) {
        if (statusFromRequest != null) {
            return statusFromRequest;
        }

        if (origin == AppointmentOrigin.TELEGRAM) {
            return AppointmentStatus.REQUESTED;
        }
        return AppointmentStatus.CONFIRMED;
    }
}
