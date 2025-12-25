package com.gearmind.domain.task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    List<Task> findByEmpresa(Long empresaId);

    List<Task> findAllWithEmpresa();

    List<Task> findByEmpleado(Long empresaId, Long empleadoId);

    Optional<Task> findById(Long id);

    void save(Task task);

    void delete(Long id);
}
