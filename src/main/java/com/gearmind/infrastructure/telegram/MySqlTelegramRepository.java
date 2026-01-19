package com.gearmind.infrastructure.telegram;

import com.gearmind.domain.telegram.*;
import com.gearmind.infrastructure.database.DataSourceFactory;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlTelegramRepository implements TelegramClientLinkRepository, TelegramConversationRepository, TelegramAppointmentRequestRepository, TelegramNotificationLogRepository, TelegramQueryRepository {

    private final DataSource dataSource;

    public MySqlTelegramRepository() {
        this(DataSourceFactory.getDataSource());
    }

    public MySqlTelegramRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<TelegramClientLink> findByChatId(long empresaId, long chatId) {
        String sql = """
                SELECT id, empresa_id, cliente_id, telegram_chat_id, telegram_username, created_at
                FROM telegram_cliente
                WHERE empresa_id = ? AND telegram_chat_id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, chatId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapClientLink(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar vínculo Telegram por chat", e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<TelegramClientLink> findByClienteId(long empresaId, long clienteId) {
        String sql = """
                SELECT id, empresa_id, cliente_id, telegram_chat_id, telegram_username, created_at
                FROM telegram_cliente
                WHERE empresa_id = ? AND cliente_id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapClientLink(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar vínculo Telegram por cliente", e);
        }

        return Optional.empty();
    }

    @Override
    public TelegramClientLink saveLink(long empresaId, long clienteId, long chatId, String username) {
        String sql = """
                INSERT INTO telegram_cliente
                    (empresa_id, cliente_id, telegram_chat_id, telegram_username, created_at)
                VALUES (?, ?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE
                    cliente_id = VALUES(cliente_id),
                    telegram_username = VALUES(telegram_username),
                    created_at = VALUES(created_at)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, clienteId);
            ps.setLong(3, chatId);
            ps.setString(4, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar vínculo Telegram", e);
        }

        return findByChatId(empresaId, chatId)
                .orElse(new TelegramClientLink(0L, empresaId, clienteId, chatId, username, LocalDateTime.now()));
    }

    @Override
    public void deleteByChatId(long empresaId, long chatId) {
        String sql = "DELETE FROM telegram_cliente WHERE empresa_id = ? AND telegram_chat_id = ?";

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, chatId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar vínculo Telegram", e);
        }
    }

    @Override
    public Optional<TelegramConversationState> findConversationByChatId(long empresaId, long chatId) {
        String sql = """
                SELECT id, empresa_id, telegram_chat_id, estado, payload, updated_at
                FROM telegram_conversacion_estado
                WHERE empresa_id = ? AND telegram_chat_id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, chatId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapConversation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar conversación Telegram", e);
        }

        return Optional.empty();
    }

    @Override
    public TelegramConversationState save(TelegramConversationState state) {
        String sql = """
                INSERT INTO telegram_conversacion_estado
                    (empresa_id, telegram_chat_id, estado, payload, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    estado = VALUES(estado),
                    payload = VALUES(payload),
                    updated_at = VALUES(updated_at)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, state.getEmpresaId());
            ps.setLong(i++, state.getChatId());
            ps.setString(i++, state.getStep().name());
            ps.setString(i++, state.getPayload());
            ps.setTimestamp(i, Timestamp.valueOf(state.getUpdatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar conversación Telegram", e);
        }

        return findConversationByChatId(state.getEmpresaId(), state.getChatId())
                .orElse(state);
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM telegram_conversacion_estado WHERE id = ?";

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al borrar conversación Telegram", e);
        }
    }

    @Override
    public TelegramAppointmentRequest save(TelegramAppointmentRequest request) {
        String sql = """
                INSERT INTO telegram_solicitud_cita
                    (empresa_id, cliente_id, vehiculo_id, telegram_chat_id, mensaje, fecha, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setLong(i++, request.getEmpresaId());
            if (request.getClienteId() != null) {
                ps.setLong(i++, request.getClienteId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }
            if (request.getVehiculoId() != null) {
                ps.setLong(i++, request.getVehiculoId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }
            ps.setLong(i++, request.getChatId());
            ps.setString(i++, request.getMensaje());
            ps.setTimestamp(i++, Timestamp.valueOf(request.getFecha()));
            ps.setString(i, request.getEstado().name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    request.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar solicitud de cita Telegram", e);
        }

        return request;
    }

    @Override
    public void save(TelegramNotificationLog log) {
        String sql = """
                INSERT INTO telegram_notificacion_log
                    (empresa_id, cliente_id, telegram_chat_id, tipo_evento, payload, enviado_at, resultado)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, log.getEmpresaId());
            ps.setLong(i++, log.getClienteId());
            ps.setLong(i++, log.getChatId());
            ps.setString(i++, log.getTipoEvento());
            ps.setString(i++, log.getPayload());
            ps.setTimestamp(i++, Timestamp.valueOf(log.getEnviadoAt()));
            ps.setString(i, log.getResultado());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar log de notificación Telegram", e);
        }
    }

    @Override
    public List<TelegramAppointmentSummary> findUpcomingAppointments(long empresaId, long clienteId, int limit) {
        String sql = """
                SELECT id, fecha_hora, estado, notas
                FROM cita
                WHERE empresa_id = ? AND cliente_id = ? AND fecha_hora >= NOW()
                ORDER BY fecha_hora ASC
                LIMIT ?
                """;

        List<TelegramAppointmentSummary> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, clienteId);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    LocalDateTime dateTime = ts != null ? ts.toLocalDateTime() : null;
                    result.add(new TelegramAppointmentSummary(rs.getLong("id"), dateTime, rs.getString("estado"), rs.getString("notas")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar citas Telegram", e);
        }

        return result;
    }

    @Override
    public List<TelegramRepairSummary> findRecentRepairs(long empresaId, long clienteId, int limit) {
        String sql = """
                SELECT id, descripcion, estado
                FROM reparacion
                WHERE empresa_id = ? AND cliente_id = ? AND estado <> 'FINALIZADA'
                ORDER BY created_at DESC
                LIMIT ?
                """;

        List<TelegramRepairSummary> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, clienteId);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new TelegramRepairSummary(rs.getLong("id"), rs.getString("descripcion"), rs.getString("estado")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar reparaciones Telegram", e);
        }

        return result;
    }

    @Override
    public List<TelegramInvoiceSummary> findRecentInvoices(long empresaId, long clienteId, int limit) {
        String sql = """
                SELECT id, numero, fecha, estado, total
                FROM factura
                WHERE empresa_id = ? AND cliente_id = ?
                ORDER BY fecha DESC
                LIMIT ?
                """;

        List<TelegramInvoiceSummary> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, clienteId);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date fecha = rs.getDate("fecha");
                    result.add(new TelegramInvoiceSummary(rs.getLong("id"), rs.getString("numero"), fecha != null ? fecha.toLocalDate() : LocalDate.now(), rs.getString("estado"), rs.getBigDecimal("total")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar facturas Telegram", e);
        }

        return result;
    }

    @Override
    public List<Integer> findBookedAppointmentHours(long empresaId, LocalDate date) {
        String sql = """
                SELECT fecha_hora
                FROM cita
                WHERE empresa_id = ? AND DATE(fecha_hora) = ? AND estado <> 'CANCELADA'
                """;

        List<Integer> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setDate(2, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    if (ts != null) {
                        result.add(ts.toLocalDateTime().getHour());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar disponibilidad de citas Telegram", e);
        }

        return result;
    }

    private TelegramClientLink mapClientLink(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long empresaId = rs.getLong("empresa_id");
        long clienteId = rs.getLong("cliente_id");
        long chatId = rs.getLong("telegram_chat_id");
        String username = rs.getString("telegram_username");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime created = ts != null ? ts.toLocalDateTime() : null;
        return new TelegramClientLink(id, empresaId, clienteId, chatId, username, created);
    }

    private TelegramConversationState mapConversation(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long empresaId = rs.getLong("empresa_id");
        long chatId = rs.getLong("telegram_chat_id");
        String estado = rs.getString("estado");
        String payload = rs.getString("payload");
        Timestamp ts = rs.getTimestamp("updated_at");
        LocalDateTime updated = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        return new TelegramConversationState(id, empresaId, chatId, TelegramConversationStep.valueOf(estado), payload, updated);
    }
}
