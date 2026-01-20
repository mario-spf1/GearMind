package com.gearmind.application.appointment;

import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentOrigin;
import com.gearmind.domain.appointment.AppointmentRepository;
import com.gearmind.domain.appointment.AppointmentStatus;
import com.gearmind.domain.appointment.OverlappingAppointmentException;
import com.gearmind.domain.user.UserRole;
import com.gearmind.application.telegram.SendTelegramNotificationUseCase;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

public class SaveAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final SendTelegramNotificationUseCase notificationUseCase;

    public SaveAppointmentUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
        this.notificationUseCase = null;
    }

    public SaveAppointmentUseCase(AppointmentRepository appointmentRepository, SendTelegramNotificationUseCase notificationUseCase) {
        this.appointmentRepository = appointmentRepository;
        this.notificationUseCase = notificationUseCase;
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
            notifyCreation(appointment);
        } else {
            Optional<Appointment> maybeExisting = appointmentRepository.findById(request.getId());
            Appointment existing = maybeExisting.orElseThrow(() -> new IllegalArgumentException("La cita indicada no existe."));
            LocalDateTime previousDateTime = existing.getDateTime();
            Long previousVehicleId = existing.getVehicleId();
            Long previousEmployeeId = existing.getEmployeeId();
            String previousNotes = existing.getNotes();
            AppointmentStatus previousStatus = existing.getStatus();
            Long previousCustomerId = existing.getCustomerId();

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
            notifyModification(existing, previousDateTime, previousVehicleId, previousEmployeeId, previousNotes, previousStatus, previousCustomerId);
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

    private void notifyCreation(Appointment appointment) {
        if (notificationUseCase == null || appointment.getCustomerId() == null) {
            return;
        }
        String when = formatDateTime(appointment.getDateTime());
        String statusLabel = mapStatusLabel(appointment.getStatus());
        String message = "Se ha registrado tu cita #" + appointment.getId() + " para el " + when + ". Estado: " + statusLabel + ".";
        String payload = "appointmentId=" + appointment.getId() + ";status=" + appointment.getStatus().name();
        notificationUseCase.execute(appointment.getCustomerId(), "CITA_CREADA", message, payload);
    }

    private void notifyModification(Appointment appointment, LocalDateTime previousDateTime, Long previousVehicleId, Long previousEmployeeId,
            String previousNotes, AppointmentStatus previousStatus, Long previousCustomerId) {
        if (notificationUseCase == null || appointment.getCustomerId() == null) {
            return;
        }
        boolean changed = !Objects.equals(previousDateTime, appointment.getDateTime())
                || !Objects.equals(previousVehicleId, appointment.getVehicleId())
                || !Objects.equals(previousEmployeeId, appointment.getEmployeeId())
                || !Objects.equals(previousNotes, appointment.getNotes())
                || !Objects.equals(previousStatus, appointment.getStatus())
                || !Objects.equals(previousCustomerId, appointment.getCustomerId());

        if (!changed) {
            return;
        }

        String when = formatDateTime(appointment.getDateTime());
        String message = "Tu cita #" + appointment.getId() + " ha sido modificada. Nueva fecha y hora: " + when + ".";
        String payload = "appointmentId=" + appointment.getId() + ";status=" + appointment.getStatus().name();
        notificationUseCase.execute(appointment.getCustomerId(), "CITA_MODIFICADA", message, payload);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "fecha por confirmar";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String mapStatusLabel(AppointmentStatus status) {
        if (status == null) {
            return "";
        }
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
