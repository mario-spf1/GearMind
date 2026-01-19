package com.gearmind.domain.telegram;

import java.util.List;
import java.time.LocalDate;

public interface TelegramQueryRepository {

    List<TelegramAppointmentSummary> findUpcomingAppointments(long empresaId, long clienteId, int limit);

    List<TelegramRepairSummary> findRecentRepairs(long empresaId, long clienteId, int limit);

    List<TelegramInvoiceSummary> findRecentInvoices(long empresaId, long clienteId, int limit);

    List<Integer> findBookedAppointmentHours(long empresaId, LocalDate date);
}
