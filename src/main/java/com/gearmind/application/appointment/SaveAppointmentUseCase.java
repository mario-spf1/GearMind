package com.gearmind.application.appointment;

import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentOrigin;
import com.gearmind.domain.appointment.AppointmentRepository;
import com.gearmind.domain.appointment.AppointmentStatus;
import com.gearmind.domain.appointment.OverlappingAppointmentException;
import com.gearmind.domain.user.UserRole;
import java.time.LocalDateTime;
import java.util.Optional;

public class SaveAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;

    public SaveAppointmentUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public void execute(SaveAppointmentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La petición de guardado de cita no puede ser nula.");
        }

        if (request.getEmpresaId() == null) {
            throw new IllegalArgumentException("La empresa es obligatoria para la cita.");
        }

        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException("El cliente es obligatorio para la cita.");
        }

        if (request.getDateTime() == null) {
            throw new IllegalArgumentException("La fecha y hora de la cita son obligatorias.");
        }

        Long empresaId = request.getEmpresaId();
        Long vehicleId = request.getVehicleId();
        LocalDateTime dateTime = request.getDateTime();
        Long excludeId = request.getId();

        boolean overlaps = appointmentRepository.existsAtDateTime(empresaId, vehicleId, dateTime, excludeId);
        if (overlaps) {
            throw new OverlappingAppointmentException("Ya existe otra cita para ese vehículo en la misma fecha y hora.");
        }

        Long resolvedEmployeeId = resolveEmployeeId(request);

        if (request.getId() == null) {
            Appointment appointment = new Appointment();
            appointment.setEmpresaId(empresaId);
            appointment.setCustomerId(request.getCustomerId());
            appointment.setVehicleId(vehicleId);
            appointment.setEmployeeId(resolvedEmployeeId);
            appointment.setDateTime(dateTime);
            AppointmentOrigin origin = defaultOrigin(request.getOrigin());
            appointment.setOrigin(origin);
            appointment.setStatus(defaultStatus(request.getStatus(), origin));
            appointment.setNotes(request.getNotes());
            appointment.setCreatedAt(LocalDateTime.now());
            appointment.setUpdatedAt(null);
            appointmentRepository.save(appointment);
            
        } else {
            Optional<Appointment> maybeExisting = appointmentRepository.findById(request.getId());
            Appointment existing = maybeExisting.orElseThrow(() -> new IllegalArgumentException("La cita indicada no existe."));

            enforcePermissionsForUpdate(existing, request);
            existing.setCustomerId(request.getCustomerId());
            existing.setVehicleId(request.getVehicleId());
            existing.setDateTime(dateTime);
            existing.setNotes(request.getNotes());
            existing.setEmployeeId(resolvedEmployeeId);

            if (request.getOrigin() != null) {
                existing.setOrigin(request.getOrigin());
            }
            if (request.getStatus() != null) {
                existing.setStatus(request.getStatus());
            }

            existing.setUpdatedAt(LocalDateTime.now());
            appointmentRepository.save(existing);
        }
    }

    private Long resolveEmployeeId(SaveAppointmentRequest request) {
        Long employeeFromRequest = request.getEmployeeId();
        Long currentUserId = request.getCurrentUserId();
        UserRole role = request.getCurrentUserRole();

        if (role == UserRole.EMPLEADO) {
            return currentUserId;
        }

        if (employeeFromRequest != null) {
            return employeeFromRequest;
        }

        return currentUserId;
    }

    private void enforcePermissionsForUpdate(Appointment existing, SaveAppointmentRequest request) {
        UserRole role = request.getCurrentUserRole();
        Long currentUserId = request.getCurrentUserId();

        if (role == UserRole.EMPLEADO) {
            Long ownerId = existing.getEmployeeId();
            if (ownerId != null && currentUserId != null && !ownerId.equals(currentUserId)) {
                throw new IllegalArgumentException("No puedes modificar citas asignadas a otros empleados.");
            }
        }
    }

    private AppointmentOrigin defaultOrigin(AppointmentOrigin originFromRequest) {
        return originFromRequest != null ? originFromRequest : AppointmentOrigin.INTERNAL;
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
