package com.gearmind.application.repair;

import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.repair.RepairRepository;
import com.gearmind.domain.repair.RepairStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public class ChangeRepairStatusUseCase {

    private final RepairRepository repairRepository;

    public ChangeRepairStatusUseCase(RepairRepository repairRepository) {
        this.repairRepository = repairRepository;
    }

    public void execute(Long repairId, Long empresaId, RepairStatus newStatus) {
        if (repairId == null) {
            throw new IllegalArgumentException("El id de la reparación es obligatorio.");
        }
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio.");
        }

        Optional<Repair> optionalRepair = repairRepository.findById(repairId);
        Repair repair = optionalRepair.orElseThrow(() -> new IllegalArgumentException("Reparación no encontrada con id: " + repairId));

        if (!empresaId.equals(repair.getEmpresaId())) {
            throw new IllegalArgumentException("La reparación no pertenece a la empresa indicada.");
        }

        repair.setEstado(newStatus);
        repair.setUpdatedAt(LocalDateTime.now());
        repairRepository.save(repair);
    }
}
