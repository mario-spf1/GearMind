package com.gearmind.application.vehicle;

import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.vehicle.VehicleRepository;

public class DeleteVehicleUseCase {

    private final VehicleRepository repository;

    public DeleteVehicleUseCase(VehicleRepository repository) {
        this.repository = repository;
    }

    public void delete(long vehicleId, long empresaId) {
        if (!AuthContext.isAdminOrSuperAdmin()) {
            throw new IllegalStateException("No tienes permisos para eliminar vehículos.");
        }
        repository.delete(vehicleId, empresaId);
    }
}
