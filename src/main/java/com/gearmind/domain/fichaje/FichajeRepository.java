package com.gearmind.domain.fichaje;

import java.util.List;
import java.util.Optional;

public interface FichajeRepository {

    Optional<Fichaje> findLastByUser(Long userId);

    List<Fichaje> findByFilters(Long empresaId, Long userId);

    void save(Fichaje fichaje);
}
