package com.gearmind.application.task;

import com.gearmind.domain.task.Task;
import com.gearmind.domain.task.TaskPriority;
import com.gearmind.domain.task.TaskRepository;
import com.gearmind.domain.task.TaskStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public class SaveTaskUseCase {

    private final TaskRepository taskRepository;

    public SaveTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void execute(SaveTaskRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La petición de tarea no puede ser nula.");
        }
        if (request.getEmpresaId() == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (request.getOrdenTrabajoId() == null) {
            throw new IllegalArgumentException("La orden de trabajo es obligatoria.");
        }
        if (request.getTitulo() == null || request.getTitulo().isBlank()) {
            throw new IllegalArgumentException("El título de la tarea es obligatorio.");
        }
        if (request.getDescripcion() == null || request.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripción de la tarea es obligatoria.");
        }

        TaskStatus status = request.getEstado() != null ? request.getEstado() : TaskStatus.PENDIENTE;
        TaskPriority prioridad = request.getPrioridad() != null ? request.getPrioridad() : TaskPriority.MEDIA;

        if (request.getId() == null) {
            Task task = new Task();
            task.setEmpresaId(request.getEmpresaId());
            task.setOrdenTrabajoId(request.getOrdenTrabajoId());
            task.setAsignadoA(request.getAsignadoA());
            task.setTitulo(request.getTitulo());
            task.setDescripcion(request.getDescripcion());
            task.setEstado(status);
            task.setPrioridad(prioridad);
            task.setFechaLimite(request.getFechaLimite());
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(null);
            taskRepository.save(task);
        } else {
            Optional<Task> existing = taskRepository.findById(request.getId());
            Task task = existing.orElseThrow(() -> new IllegalArgumentException("La tarea indicada no existe."));
            task.setOrdenTrabajoId(request.getOrdenTrabajoId());
            task.setAsignadoA(request.getAsignadoA());
            task.setTitulo(request.getTitulo());
            task.setDescripcion(request.getDescripcion());
            task.setEstado(status);
            task.setPrioridad(prioridad);
            task.setFechaLimite(request.getFechaLimite());
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);
        }
    }
}
