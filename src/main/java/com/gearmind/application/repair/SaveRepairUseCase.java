package com.gearmind.application.repair;

import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.repair.RepairRepository;
import com.gearmind.domain.repair.RepairStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public class SaveRepairUseCase {

    private final RepairRepository repairRepository;

    public SaveRepairUseCase(RepairRepository repairRepository) {
        this.repairRepository = repairRepository;
    }

    public void execute(SaveRepairRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La petición de reparación no puede ser nula.");
        }
        if (request.getEmpresaId() == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (request.getClienteId() == null) {
            throw new IllegalArgumentException("El cliente es obligatorio.");
        }
        if (request.getVehiculoId() == null) {
            throw new IllegalArgumentException("El vehículo es obligatorio.");
        }
        if (request.getDescripcion() == null || request.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripción de la reparación es obligatoria.");
        }

        RepairStatus status = request.getEstado() != null ? request.getEstado() : RepairStatus.ABIERTA;
        BigDecimal importeEstimado = request.getImporteEstimado();
        BigDecimal importeFinal = request.getImporteFinal();

        if (request.getId() == null) {
            Repair repair = new Repair();
            repair.setEmpresaId(request.getEmpresaId());
            repair.setCitaId(request.getCitaId());
            repair.setClienteId(request.getClienteId());
            repair.setVehiculoId(request.getVehiculoId());
            repair.setDescripcion(request.getDescripcion());
            repair.setEstado(status);
            repair.setImporteEstimado(importeEstimado);
            repair.setImporteFinal(importeFinal);
            repair.setCreatedAt(LocalDateTime.now());
            repair.setUpdatedAt(null);
            repairRepository.save(repair);
        } else {
            Optional<Repair> maybeRepair = repairRepository.findById(request.getId());
            Repair existing = maybeRepair.orElseThrow(() -> new IllegalArgumentException("La reparación indicada no existe."));
            existing.setCitaId(request.getCitaId());
            existing.setClienteId(request.getClienteId());
            existing.setVehiculoId(request.getVehiculoId());
            existing.setDescripcion(request.getDescripcion());
            existing.setImporteEstimado(importeEstimado);
            existing.setImporteFinal(importeFinal);
            if (request.getEstado() != null) {
                existing.setEstado(request.getEstado());
            }
            existing.setUpdatedAt(LocalDateTime.now());
            repairRepository.save(existing);
        }
    }
}
