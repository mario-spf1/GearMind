package com.gearmind.domain.vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {

    List<Vehicle> findByEmpresaId(Long empresaId);

    List<Vehicle> findAllWithEmpresa();

    Optional<Vehicle> findById(Long id);

    Vehicle save(Vehicle vehicle);

    boolean existsMatriculaInEmpresa(Long empresaId, String matricula, Long excludeId);
}
