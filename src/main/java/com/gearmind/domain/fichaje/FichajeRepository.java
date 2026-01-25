package com.gearmind.domain.fichaje;

import java.util.List;
import java.util.Optional;

public interface FichajeRepository {

    Optional<Fichaje> findLastByUser(Long userId);

    List<Fichaje> findByFilters(Long empresaId, Long userId);

    List<Fichaje> findByFilters(Long empresaId, Long userId, java.time.LocalDate date);

    List<Fichaje> findByUserAndDate(Long userId, java.time.LocalDate date);

    void save(Fichaje fichaje);
}
