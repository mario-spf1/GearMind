package com.gearmind.application.inventory;

import com.gearmind.domain.inventory.InventoryMovement;
import com.gearmind.domain.inventory.InventoryMovementRepository;
import com.gearmind.domain.inventory.InventoryMovementType;
import com.gearmind.domain.product.Product;
import com.gearmind.domain.product.ProductRepository;

import java.time.LocalDateTime;

public class AdjustInventoryUseCase {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;

    public AdjustInventoryUseCase(ProductRepository productRepository, InventoryMovementRepository movementRepository) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    public void execute(long empresaId, long productoId, InventoryMovementType tipo, int cantidad, String referencia, String notas) {
        if (empresaId <= 0) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (productoId <= 0) {
            throw new IllegalArgumentException("El producto es obligatorio.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio.");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        Product product = productRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el producto."));

        if (product.getEmpresaId() == null || product.getEmpresaId() != empresaId) {
            throw new IllegalArgumentException("El producto no pertenece a la empresa.");
        }

        int current = product.getStock() == null ? 0 : product.getStock();
        int delta = tipo == InventoryMovementType.ENTRADA ? cantidad : -cantidad;
        int newStock = current + delta;
        if (newStock < 0) {
            throw new IllegalArgumentException("No hay stock suficiente para completar la operación.");
        }

        productRepository.updateStock(productoId, empresaId, newStock);

        InventoryMovement movement = new InventoryMovement();
        movement.setEmpresaId(empresaId);
        movement.setProductoId(productoId);
        movement.setTipo(tipo);
        movement.setCantidad(cantidad);
        movement.setReferencia(referencia);
        movement.setNotas(notas);
        movement.setCreatedAt(LocalDateTime.now());
        movementRepository.create(movement);
    }
}
