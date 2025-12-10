package com.gearmind.domain.appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {

    List<Appointment> findByEmpresa(Long empresaId);

    Optional<Appointment> findById(Long id);

    void save(Appointment appointment);

    void delete(Long id);

    boolean existsAtDateTime(Long empresaId, Long vehicleId, LocalDateTime dateTime, Long excludeId);
}
