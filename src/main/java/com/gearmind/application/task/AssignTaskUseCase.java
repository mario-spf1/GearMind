package com.gearmind.application.task;

import com.gearmind.application.message.SendMessageUseCase;
import com.gearmind.domain.task.Task;
import com.gearmind.domain.task.TaskRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public class AssignTaskUseCase {

    private final TaskRepository taskRepository;
    private final SendMessageUseCase internalMessageUseCase;

    public AssignTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        this.internalMessageUseCase = null;
    }

    public AssignTaskUseCase(TaskRepository taskRepository, SendMessageUseCase internalMessageUseCase) {
        this.taskRepository = taskRepository;
        this.internalMessageUseCase = internalMessageUseCase;
    }

    public void execute(Long taskId, Long empresaId, Long empleadoId, Long senderId) {
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

        Long previousEmployeeId = task.getAsignadoA();
        task.setAsignadoA(empleadoId);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        notifyAssignment(task, senderId, previousEmployeeId);
    }

    private void notifyAssignment(Task task, Long senderId, Long previousEmployeeId) {
        if (internalMessageUseCase == null) {
            return;
        }
        Long assignedTo = task.getAsignadoA();
        if (assignedTo == null || senderId == null) {
            return;
        }
        if (assignedTo.equals(senderId)) {
            return;
        }
        if (previousEmployeeId != null && previousEmployeeId.equals(assignedTo)) {
            return;
        }
        String message = "Se te ha asignado la tarea #" + task.getId() + ": " + task.getTitulo() + ".";
        internalMessageUseCase.execute(task.getEmpresaId(), senderId, assignedTo, message);
    }

}
