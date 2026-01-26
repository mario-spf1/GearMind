package com.gearmind.domain.inventory;

import java.util.List;

public interface InventoryMovementRepository {

    InventoryMovement create(InventoryMovement movement);

    List<InventoryMovement> findByEmpresa(long empresaId);

    List<InventoryMovement> findAllWithEmpresa();

    List<InventoryMovement> findByEmpresaAndReferencia(long empresaId, String referencia);
}
