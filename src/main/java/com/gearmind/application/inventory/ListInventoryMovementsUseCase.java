package com.gearmind.application.inventory;

import com.gearmind.domain.inventory.InventoryMovement;
import com.gearmind.domain.inventory.InventoryMovementRepository;

import java.util.List;

public class ListInventoryMovementsUseCase {

    private final InventoryMovementRepository repository;

    public ListInventoryMovementsUseCase(InventoryMovementRepository repository) {
        this.repository = repository;
    }

    public List<InventoryMovement> listByEmpresa(long empresaId) {
        if (empresaId <= 0) {
            throw new IllegalArgumentException("empresaId debe ser > 0");
        }
        return repository.findByEmpresa(empresaId);
    }

    public List<InventoryMovement> listAllWithEmpresa() {
        return repository.findAllWithEmpresa();
    }
}
