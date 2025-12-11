package com.gearmind.application.vehicle;

import com.gearmind.application.common.AuthContext;
import com.gearmind.common.exception.DuplicateException;
import com.gearmind.common.exception.ValidationException;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.domain.vehicle.VehicleRepository;

import java.util.Objects;

public class SaveVehicleUseCase {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;

    public SaveVehicleUseCase(VehicleRepository vehicleRepository, CustomerRepository customerRepository) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
    }

    public Vehicle execute(SaveVehicleRequest request) {
        long empresaId = AuthContext.getEmpresaId();

        validate(request, empresaId);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(request.getId());
        vehicle.setEmpresaId(empresaId);
        vehicle.setClienteId(request.getClienteId());
        vehicle.setMatricula(request.getMatricula().trim().toUpperCase());
        vehicle.setMarca(request.getMarca());
        vehicle.setModelo(request.getModelo());
        vehicle.setYear(request.getYear());
        vehicle.setVin(request.getVin());
        return vehicleRepository.save(vehicle);
    }

    private void validate(SaveVehicleRequest request, long empresaId) {
        if (request.getClienteId() == null) {
            throw new ValidationException("Debe seleccionar un cliente para el vehículo.");
        }
        if (request.getMatricula() == null || request.getMatricula().isBlank()) {
            throw new ValidationException("La matrícula es obligatoria.");
        }

        var customerOpt = customerRepository.findById(request.getClienteId());
        if (customerOpt.isEmpty() || !Objects.equals(customerOpt.get().getEmpresaId(), empresaId)) {
            throw new ValidationException("El cliente seleccionado no pertenece a tu empresa.");
        }

        Long excludeId = request.getId();
        boolean exists = vehicleRepository.existsMatriculaInEmpresa(empresaId, request.getMatricula().trim().toUpperCase(), excludeId);
        if (exists) {
            throw new DuplicateException("Ya existe un vehículo con esa matrícula en la empresa.");
        }

        if (request.getYear() != null && (request.getYear() < 1950 || request.getYear() > 2100)) {
            throw new ValidationException("El año del vehículo no es válido.");
        }
    }
}
