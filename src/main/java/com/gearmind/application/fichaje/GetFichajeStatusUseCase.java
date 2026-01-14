package com.gearmind.application.fichaje;

import com.gearmind.domain.fichaje.Fichaje;
import com.gearmind.domain.fichaje.FichajeMovimiento;
import com.gearmind.domain.fichaje.FichajeRepository;
import com.gearmind.domain.fichaje.FichajeStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class GetFichajeStatusUseCase {

    private final FichajeRepository fichajeRepository;

    public GetFichajeStatusUseCase(FichajeRepository fichajeRepository) {
        this.fichajeRepository = fichajeRepository;
    }

    public Optional<FichajeStatus> execute(Long userId) {
        Optional<Fichaje> last = fichajeRepository.findLastByUser(userId);
        if (last.isEmpty()) {
            return Optional.empty();
        }
        Fichaje fichaje = last.get();
        LocalDateTime fecha = fichaje.getFecha();
        Duration worked = null;
        if (fichaje.getMovimiento() == FichajeMovimiento.ENTRADA && fecha != null) {
            worked = Duration.between(fecha, LocalDateTime.now());
        }
        return Optional.of(new FichajeStatus(fichaje.getMovimiento(), fecha, worked));
    }
}
