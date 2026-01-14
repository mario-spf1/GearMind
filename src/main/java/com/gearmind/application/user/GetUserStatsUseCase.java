package com.gearmind.application.user;

import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.task.TaskRepository;
import com.gearmind.domain.repair.RepairRepository;

public class GetUserStatsUseCase {

    private final TaskRepository taskRepository;
    private final RepairRepository repairRepository;

    public GetUserStatsUseCase(TaskRepository taskRepository, RepairRepository repairRepository) {
        this.taskRepository = taskRepository;
        this.repairRepository = repairRepository;
    }

    public UserStats execute(Long userId) {
        long empresaId = AuthContext.getEmpresaId();
        int tareasAsignadas = taskRepository.findByEmpleado(empresaId, userId).size();
        int reparacionesEmpresa = repairRepository.findByEmpresa(empresaId).size();
        return new UserStats(tareasAsignadas, reparacionesEmpresa);
    }
}
