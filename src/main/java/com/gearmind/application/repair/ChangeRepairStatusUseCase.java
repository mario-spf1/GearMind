package com.gearmind.application.repair;

import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.repair.RepairRepository;
import com.gearmind.domain.repair.RepairStatus;
import com.gearmind.application.telegram.SendTelegramNotificationUseCase;

import java.time.LocalDateTime;
import java.util.Optional;

public class ChangeRepairStatusUseCase {

    private final RepairRepository repairRepository;
    private final SendTelegramNotificationUseCase notificationUseCase;

    public ChangeRepairStatusUseCase(RepairRepository repairRepository) {
        this.repairRepository = repairRepository;
        this.notificationUseCase = null;
    }

    public ChangeRepairStatusUseCase(RepairRepository repairRepository, SendTelegramNotificationUseCase notificationUseCase) {
        this.repairRepository = repairRepository;
        this.notificationUseCase = notificationUseCase;
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
        if (notificationUseCase != null && repair.getClienteId() != null) {
            String statusLabel = mapStatusLabel(newStatus);
            String description = repair.getDescripcion() != null ? repair.getDescripcion() : "";
            String message = "Tu reparación #" + repair.getId() + " " + description + " ha cambiado a estado: " + statusLabel + ".";
            String payload = "repairId=" + repair.getId() + ";status=" + newStatus.name();
            notificationUseCase.execute(repair.getClienteId(), "REPARACION_ESTADO", message, payload);
        }
    }

    private String mapStatusLabel(RepairStatus status) {
        return switch (status) {
            case ABIERTA ->
                "Abierta";
            case EN_PROCESO ->
                "En proceso";
            case FINALIZADA ->
                "Finalizada";
            case FACTURADA ->
                "Facturada";
            case CANCELADA ->
                "Cancelada";
        };

    }
}
