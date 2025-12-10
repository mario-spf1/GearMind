package com.gearmind.infrastructure.appointment;

import com.gearmind.domain.appointment.Appointment;
import com.gearmind.domain.appointment.AppointmentOrigin;
import com.gearmind.domain.appointment.AppointmentRepository;
import com.gearmind.domain.appointment.AppointmentStatus;
import com.gearmind.infrastructure.database.DataSourceFactory;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlAppointmentRepository implements AppointmentRepository {

    private final DataSource dataSource;

    public MySqlAppointmentRepository() {
        this.dataSource = DataSourceFactory.getDataSource();
    }

    @Override
    public List<Appointment> findByEmpresa(Long empresaId) {
        String sql = """
                SELECT id,
                    empresa_id,
                    empleado_id,
                    cliente_id,
                    vehiculo_id,
                    fecha_hora,
                    estado,
                    origen,
                    notas,
                    created_at,
                    updated_at
                FROM cita
                WHERE empresa_id = ?
                ORDER BY fecha_hora ASC
                """;

        List<Appointment> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, empresaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar citas", e);
        }

        return result;
    }

    @Override
    public Optional<Appointment> findById(Long id) {
        String sql = """
                SELECT id,
                       empresa_id,
                       empleado_id,
                       cliente_id,
                       vehiculo_id,
                       fecha_hora,
                       estado,
                       origen,
                       notas,
                       created_at,
                       updated_at
                FROM cita
                WHERE id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar cita por id", e);
        }
    }

    @Override
    public void save(Appointment appointment) {
        if (appointment.getId() == null) {
            insert(appointment);
        } else {
            update(appointment);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM cita WHERE id = ?";

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al borrar cita", e);
        }
    }

    @Override
    public boolean existsAtDateTime(Long empresaId,
            Long vehicleId,
            LocalDateTime dateTime,
            Long excludeId) {
        StringBuilder sb = new StringBuilder("""
                SELECT COUNT(*)
                FROM cita
                WHERE empresa_id = ?
                  AND fecha_hora = ?
                """);

        if (vehicleId != null) {
            sb.append(" AND vehiculo_id = ? ");
        } else {
            sb.append(" AND vehiculo_id IS NULL ");
        }

        if (excludeId != null) {
            sb.append(" AND id <> ? ");
        }

        String sql = sb.toString();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            int idx = 1;
            ps.setLong(idx++, empresaId);
            ps.setTimestamp(idx++, Timestamp.valueOf(dateTime));

            if (vehicleId != null) {
                ps.setLong(idx++, vehicleId);
            }

            if (excludeId != null) {
                ps.setLong(idx, excludeId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al comprobar solape de cita", e);
        }
    }

    private void insert(Appointment appointment) {
        String sql = """
                INSERT INTO cita
                    (empresa_id, empleado_id, cliente_id, vehiculo_id,
                     fecha_hora, estado, origen, notas)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int i = 1;
            ps.setLong(i++, appointment.getEmpresaId());

            if (appointment.getEmployeeId() != null) {
                ps.setLong(i++, appointment.getEmployeeId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }

            ps.setLong(i++, appointment.getCustomerId());

            if (appointment.getVehicleId() != null) {
                ps.setLong(i++, appointment.getVehicleId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }

            ps.setTimestamp(i++, Timestamp.valueOf(appointment.getDateTime()));
            ps.setString(i++, mapStatusToDb(appointment.getStatus()));
            ps.setString(i++, mapOriginToDb(appointment.getOrigin()));
            ps.setString(i, appointment.getNotes());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    appointment.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar cita", e);
        }
    }

    private void update(Appointment appointment) {
        String sql = """
                UPDATE cita
                SET empresa_id = ?,
                    empleado_id = ?,
                    cliente_id = ?,
                    vehiculo_id = ?,
                    fecha_hora = ?,
                    estado = ?,
                    origen = ?,
                    notas = ?
                WHERE id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            int i = 1;
            ps.setLong(i++, appointment.getEmpresaId());

            if (appointment.getEmployeeId() != null) {
                ps.setLong(i++, appointment.getEmployeeId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }

            ps.setLong(i++, appointment.getCustomerId());

            if (appointment.getVehicleId() != null) {
                ps.setLong(i++, appointment.getVehicleId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }

            ps.setTimestamp(i++, Timestamp.valueOf(appointment.getDateTime()));
            ps.setString(i++, mapStatusToDb(appointment.getStatus()));
            ps.setString(i++, mapOriginToDb(appointment.getOrigin()));
            ps.setString(i++, appointment.getNotes());
            ps.setLong(i, appointment.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar cita", e);
        }
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long empresaId = rs.getLong("empresa_id");

        Long employeeId = rs.getLong("empleado_id");
        if (rs.wasNull()) {
            employeeId = null;
        }

        Long customerId = rs.getLong("cliente_id");

        Long vehicleId = rs.getLong("vehiculo_id");
        if (rs.wasNull()) {
            vehicleId = null;
        }

        Timestamp tsFecha = rs.getTimestamp("fecha_hora");
        LocalDateTime dateTime = tsFecha != null ? tsFecha.toLocalDateTime() : null;
        AppointmentStatus status = mapStatusFromDb(rs.getString("estado"));
        AppointmentOrigin origin = mapOriginFromDb(rs.getString("origen"));
        String notes = rs.getString("notas");
        Timestamp tsCreated = rs.getTimestamp("created_at");
        Timestamp tsUpdated = rs.getTimestamp("updated_at");
        LocalDateTime createdAt = tsCreated != null ? tsCreated.toLocalDateTime() : null;
        LocalDateTime updatedAt = tsUpdated != null ? tsUpdated.toLocalDateTime() : null;

        return new Appointment(id, empresaId, employeeId, customerId, vehicleId, dateTime, status, origin, notes, createdAt, updatedAt);
    }

    private AppointmentStatus mapStatusFromDb(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        return switch (dbValue) {
            case "SOLICITADA" ->
                AppointmentStatus.REQUESTED;
            case "CONFIRMADA" ->
                AppointmentStatus.CONFIRMED;
            case "CANCELADA" ->
                AppointmentStatus.CANCELLED;
            case "COMPLETADA" ->
                AppointmentStatus.COMPLETED;
            default ->
                throw new IllegalArgumentException("Estado de cita desconocido: " + dbValue);
        };
    }

    private String mapStatusToDb(AppointmentStatus status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case REQUESTED ->
                "SOLICITADA";
            case CONFIRMED ->
                "CONFIRMADA";
            case CANCELLED ->
                "CANCELADA";
            case COMPLETED ->
                "COMPLETADA";
        };
    }

    private AppointmentOrigin mapOriginFromDb(String dbValue) {
        if (dbValue == null) {
            return AppointmentOrigin.INTERNAL;
        }
        return switch (dbValue) {
            case "INTERNO" ->
                AppointmentOrigin.INTERNAL;
            case "TELEGRAM" ->
                AppointmentOrigin.TELEGRAM;
            default ->
                throw new IllegalArgumentException("Origen de cita desconocido: " + dbValue);
        };
    }

    private String mapOriginToDb(AppointmentOrigin origin) {
        if (origin == null) {
            return "INTERNO";
        }

        return switch (origin) {
            case INTERNAL ->
                "INTERNO";
            case TELEGRAM ->
                "TELEGRAM";
        };
    }
}
