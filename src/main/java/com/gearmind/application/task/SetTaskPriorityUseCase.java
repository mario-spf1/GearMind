package com.gearmind.application.task;

import com.gearmind.domain.task.Task;
import com.gearmind.domain.task.TaskPriority;
import com.gearmind.domain.task.TaskRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public class SetTaskPriorityUseCase {

    private final TaskRepository taskRepository;

    public SetTaskPriorityUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void execute(Long taskId, Long empresaId, TaskPriority priority) {
        if (taskId == null) {
            throw new IllegalArgumentException("El id de la tarea es obligatorio.");
        }
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (priority == null) {
            throw new IllegalArgumentException("La prioridad es obligatoria.");
        }

        Optional<Task> optionalTask = taskRepository.findById(taskId);
        Task task = optionalTask.orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + taskId));

        if (!empresaId.equals(task.getEmpresaId())) {
            throw new IllegalArgumentException("La tarea no pertenece a la empresa indicada.");
        }

        task.setPrioridad(priority);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
    }
}
