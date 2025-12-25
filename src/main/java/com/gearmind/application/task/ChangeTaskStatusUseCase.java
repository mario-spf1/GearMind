package com.gearmind.application.task;

import com.gearmind.domain.task.Task;
import com.gearmind.domain.task.TaskRepository;
import com.gearmind.domain.task.TaskStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public class ChangeTaskStatusUseCase {

    private final TaskRepository taskRepository;

    public ChangeTaskStatusUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void execute(Long taskId, Long empresaId, TaskStatus newStatus) {
        if (taskId == null) {
            throw new IllegalArgumentException("El id de la tarea es obligatorio.");
        }
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio.");
        }

        Optional<Task> optionalTask = taskRepository.findById(taskId);
        Task task = optionalTask.orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + taskId));

        if (!empresaId.equals(task.getEmpresaId())) {
            throw new IllegalArgumentException("La tarea no pertenece a la empresa indicada.");
        }

        task.setEstado(newStatus);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
    }
}
