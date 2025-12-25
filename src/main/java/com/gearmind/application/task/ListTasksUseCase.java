package com.gearmind.application.task;

import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.task.Task;
import com.gearmind.domain.task.TaskRepository;

import java.util.List;

public class ListTasksUseCase {

    private final TaskRepository taskRepository;

    public ListTasksUseCase(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> execute() {
        if (AuthContext.isSuperAdmin()) {
            return taskRepository.findAllWithEmpresa();
        }

        long empresaId = AuthContext.getEmpresaId();
        if (AuthContext.isEmpleado()) {
            long empleadoId = AuthContext.getCurrentUser().getId();
            return taskRepository.findByEmpleado(empresaId, empleadoId);
        }

        return taskRepository.findByEmpresa(empresaId);
    }
}
