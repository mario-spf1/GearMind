package com.gearmind.application.fichaje;

import com.gearmind.domain.fichaje.Fichaje;
import com.gearmind.domain.fichaje.FichajeMovimiento;
import com.gearmind.domain.fichaje.FichajeRepository;

import java.time.LocalDateTime;

public class RegistrarFichajeUseCase {

    private final FichajeRepository fichajeRepository;

    public RegistrarFichajeUseCase(FichajeRepository fichajeRepository) {
        this.fichajeRepository = fichajeRepository;
    }

    public Fichaje registrar(Long empresaId, Long userId, FichajeMovimiento movimiento) {
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio.");
        }
        if (movimiento == null) {
            throw new IllegalArgumentException("El movimiento es obligatorio.");
        }
        Fichaje fichaje = new Fichaje();
        fichaje.setEmpresaId(empresaId);
        fichaje.setUserId(userId);
        fichaje.setMovimiento(movimiento);
        fichaje.setFecha(LocalDateTime.now());
        fichajeRepository.save(fichaje);
        return fichaje;
    }
}
