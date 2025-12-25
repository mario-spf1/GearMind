package com.gearmind.application.repair;

import com.gearmind.application.common.AuthContext;
import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.repair.RepairRepository;

import java.util.List;

public class ListRepairsUseCase {

    private final RepairRepository repairRepository;

    public ListRepairsUseCase(RepairRepository repairRepository) {
        this.repairRepository = repairRepository;
    }

    public List<Repair> execute() {
        if (AuthContext.isSuperAdmin()) {
            return repairRepository.findAllWithEmpresa();
        }
        long empresaId = AuthContext.getEmpresaId();
        return repairRepository.findByEmpresa(empresaId);
    }
}
