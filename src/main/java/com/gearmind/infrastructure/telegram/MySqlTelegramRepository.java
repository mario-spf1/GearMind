package com.gearmind.infrastructure.telegram;

import com.gearmind.domain.telegram.*;
import com.gearmind.infrastructure.database.DataSourceFactory;
import com.gearmind.domain.vehicle.Vehicle;
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
    public List<TelegramAppointmentRequest> findByEmpresaIdAndStatus(long empresaId, TelegramAppointmentRequestStatus status) {
        String sql = """
                SELECT id, empresa_id, cliente_id, vehiculo_id, telegram_chat_id, mensaje, fecha, estado
                FROM telegram_solicitud_cita
                WHERE empresa_id = ? AND estado = ?
                ORDER BY fecha DESC
                """;

        List<TelegramAppointmentRequest> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, status.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapAppointmentRequest(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar solicitudes de cita Telegram", e);
        }

        return result;
    }

    @Override
    public void updateStatus(long requestId, TelegramAppointmentRequestStatus status) {
        String sql = """
                UPDATE telegram_solicitud_cita
                SET estado = ?, updated_at = NOW()
                WHERE id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar estado de solicitud de cita Telegram", e);
        }
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
            ps.setString(i++, log.getTipoEvento() != null ? log.getTipoEvento() : "DESCONOCIDO");
            ps.setString(i++, log.getPayload() != null ? log.getPayload() : "");
            ps.setTimestamp(i++, Timestamp.valueOf(log.getEnviadoAt()));
            ps.setString(i, log.getResultado() != null ? log.getResultado() : "ERROR");
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

    private TelegramAppointmentRequest mapAppointmentRequest(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        long empresaId = rs.getLong("empresa_id");
        Long clienteId = rs.getLong("cliente_id");
        if (rs.wasNull()) {
            clienteId = null;
        }
        Long vehiculoId = rs.getLong("vehiculo_id");
        if (rs.wasNull()) {
            vehiculoId = null;
        }
        long chatId = rs.getLong("telegram_chat_id");
        String mensaje = rs.getString("mensaje");
        Timestamp tsFecha = rs.getTimestamp("fecha");
        LocalDateTime fecha = tsFecha != null ? tsFecha.toLocalDateTime() : null;
        TelegramAppointmentRequestStatus estado = TelegramAppointmentRequestStatus.valueOf(rs.getString("estado"));
        return new TelegramAppointmentRequest(id, empresaId, clienteId, vehiculoId, chatId, mensaje, fecha, estado);
    }

    @Override
    public List<TelegramRepairSummary> findRecentRepairs(long empresaId, long clienteId, int limit) {
        String sql = """
                SELECT r.id,
                    r.descripcion,
                    r.estado,
                    v.matricula,
                    v.marca,
                    v.modelo
                FROM reparacion r
                JOIN vehiculo v ON v.id = r.vehiculo_id
                WHERE r.empresa_id = ? AND r.cliente_id = ? AND r.estado <> 'FINALIZADA'
                ORDER BY r.created_at DESC
                LIMIT ?
                """;

        List<TelegramRepairSummary> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, clienteId);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String vehiculo = formatVehicle(rs.getString("matricula"), rs.getString("marca"), rs.getString("modelo"));
                    result.add(new TelegramRepairSummary(rs.getLong("id"), rs.getString("descripcion"), rs.getString("estado"), vehiculo));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar reparaciones Telegram", e);
        }

        return result;
    }

    private String formatVehicle(String matricula, String marca, String modelo) {
        StringBuilder sb = new StringBuilder();
        if (matricula != null && !matricula.isBlank()) {
            sb.append(matricula);
        }
        if (marca != null && !marca.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" - ");
            }
            sb.append(marca);
        }
        if (modelo != null && !modelo.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(modelo);
        }
        return sb.isEmpty() ? "Vehículo N/D" : sb.toString();
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

    @Override
    public List<Vehicle> findVehiclesByCliente(long empresaId, long clienteId) {
        String sql = """
                SELECT id, empresa_id, cliente_id, matricula, marca, modelo, year, vin, created_at, updated_at
                FROM vehiculo
                WHERE empresa_id = ? AND cliente_id = ?
                ORDER BY matricula ASC
                """;

        List<Vehicle> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Vehicle vehicle = new Vehicle();
                    vehicle.setId(rs.getLong("id"));
                    vehicle.setEmpresaId(rs.getLong("empresa_id"));
                    vehicle.setClienteId(rs.getLong("cliente_id"));
                    vehicle.setMatricula(rs.getString("matricula"));
                    vehicle.setMarca(rs.getString("marca"));
                    vehicle.setModelo(rs.getString("modelo"));
                    int year = rs.getInt("year");
                    if (!rs.wasNull()) {
                        vehicle.setYear(year);
                    }
                    vehicle.setVin(rs.getString("vin"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) {
                        vehicle.setCreatedAt(created.toLocalDateTime());
                    }
                    Timestamp updated = rs.getTimestamp("updated_at");
                    if (updated != null) {
                        vehicle.setUpdatedAt(updated.toLocalDateTime());
                    }
                    result.add(vehicle);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar vehículos Telegram", e);
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
