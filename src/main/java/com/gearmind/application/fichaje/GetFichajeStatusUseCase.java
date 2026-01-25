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
        Duration worked = calculateWorkedToday(userId);
        return Optional.of(new FichajeStatus(fichaje.getMovimiento(), fecha, worked));
    }

    private Duration calculateWorkedToday(Long userId) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.List<Fichaje> fichajes = fichajeRepository.findByUserAndDate(userId, today);
        if (fichajes.isEmpty()) {
            return Duration.ZERO;
        }

        Duration total = Duration.ZERO;
        LocalDateTime open = null;
        for (Fichaje f : fichajes) {
            if (f.getFecha() == null) {
                continue;
            }
            if (f.getMovimiento() == FichajeMovimiento.ENTRADA) {
                open = f.getFecha();
            } else if (f.getMovimiento() == FichajeMovimiento.SALIDA) {
                if (open != null) {
                    total = total.plus(Duration.between(open, f.getFecha()));
                    open = null;
                }
            }
        }

        if (open != null) {
            total = total.plus(Duration.between(open, LocalDateTime.now()));
        }

        return total;
    }
}
