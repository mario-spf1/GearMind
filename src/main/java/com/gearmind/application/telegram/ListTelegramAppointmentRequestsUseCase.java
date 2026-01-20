package com.gearmind.application.telegram;

import com.gearmind.domain.telegram.TelegramAppointmentRequest;
import com.gearmind.domain.telegram.TelegramAppointmentRequestRepository;
import com.gearmind.domain.telegram.TelegramAppointmentRequestStatus;

import java.util.List;

public class ListTelegramAppointmentRequestsUseCase {

    private final TelegramAppointmentRequestRepository repository;

    public ListTelegramAppointmentRequestsUseCase(TelegramAppointmentRequestRepository repository) {
        this.repository = repository;
    }

    public List<TelegramAppointmentRequest> execute(long empresaId, TelegramAppointmentRequestStatus status) {
        return repository.findByEmpresaIdAndStatus(empresaId, status);
    }
}
