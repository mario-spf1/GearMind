package com.gearmind.domain.telegram;

import java.util.List;

public interface TelegramQueryRepository {

    List<TelegramAppointmentSummary> findUpcomingAppointments(long empresaId, long clienteId, int limit);

    List<TelegramRepairSummary> findRecentRepairs(long empresaId, long clienteId, int limit);

    List<TelegramInvoiceSummary> findRecentInvoices(long empresaId, long clienteId, int limit);
}
