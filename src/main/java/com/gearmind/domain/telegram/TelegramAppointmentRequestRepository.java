package com.gearmind.domain.telegram;

public interface TelegramAppointmentRequestRepository {

    TelegramAppointmentRequest save(TelegramAppointmentRequest request);

    java.util.List<TelegramAppointmentRequest> findByEmpresaIdAndStatus(long empresaId, TelegramAppointmentRequestStatus status);

    void updateStatus(long requestId, TelegramAppointmentRequestStatus status);
}
