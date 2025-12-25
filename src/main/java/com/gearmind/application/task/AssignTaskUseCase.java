package com.gearmind.application.task;

import com.gearmind.domain.task.Task;
import com.gearmind.domain.task.TaskRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public class AssignTaskUseCase {

    private final TaskRepository taskRepository;

    public AssignTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void execute(Long taskId, Long empresaId, Long empleadoId) {
        if (taskId == null) {
            throw new IllegalArgumentException("El id de la tarea es obligatorio.");
        }
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (empleadoId == null) {
            throw new IllegalArgumentException("El empleado es obligatorio.");
        }

        Optional<Task> optionalTask = taskRepository.findById(taskId);
        Task task = optionalTask.orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + taskId));

        if (!empresaId.equals(task.getEmpresaId())) {
            throw new IllegalArgumentException("La tarea no pertenece a la empresa indicada.");
        }

        task.setAsignadoA(empleadoId);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
    }
}
