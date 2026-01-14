package com.gearmind.domain.fichaje;

import java.time.Duration;
import java.time.LocalDateTime;

public record FichajeStatus(FichajeMovimiento movimientoActual, LocalDateTime fechaUltimoMovimiento, Duration tiempoTrabajado) {

}
