package com.gearmind.application.vehicle;

import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.domain.vehicle.VehicleRepository;

import java.util.List;

public class ListVehiclesUseCase {

    private final VehicleRepository vehicleRepository;

    public ListVehiclesUseCase(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<Vehicle> execute() {
        long empresaId = AuthContext.getEmpresaId();
        return vehicleRepository.findByEmpresaId(empresaId);
    }
}
