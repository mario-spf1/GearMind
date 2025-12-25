package com.gearmind.domain.repair;

import java.util.List;
import java.util.Optional;

public interface RepairRepository {

    List<Repair> findByEmpresa(Long empresaId);

    List<Repair> findAllWithEmpresa();

    Optional<Repair> findById(Long id);

    void save(Repair repair);

    void delete(Long id);
}
