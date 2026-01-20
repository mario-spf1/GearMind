package com.gearmind.application.telegram;

import com.gearmind.domain.telegram.TelegramAppointmentRequestRepository;
import com.gearmind.domain.telegram.TelegramAppointmentRequestStatus;

public class UpdateTelegramAppointmentRequestStatusUseCase {

    private final TelegramAppointmentRequestRepository repository;

    public UpdateTelegramAppointmentRequestStatusUseCase(TelegramAppointmentRequestRepository repository) {
        this.repository = repository;
    }

    public void execute(long requestId, TelegramAppointmentRequestStatus status) {
        repository.updateStatus(requestId, status);
    }
}
