package com.gearmind.application.repair;

import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.repair.RepairRepository;

import java.util.Optional;

public class GetRepairUseCase {

    private final RepairRepository repairRepository;

    public GetRepairUseCase(RepairRepository repairRepository) {
        this.repairRepository = repairRepository;
    }

    public Repair execute(Long repairId) {
        if (repairId == null) {
            throw new IllegalArgumentException("El id de la reparación es obligatorio.");
        }

        Optional<Repair> optionalRepair = repairRepository.findById(repairId);
        return optionalRepair.orElseThrow(() -> new IllegalArgumentException("Reparación no encontrada con id: " + repairId));
    }
}
