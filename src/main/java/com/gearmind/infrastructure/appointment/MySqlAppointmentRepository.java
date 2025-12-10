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
        String sql = "SELECT id, empresa_id, cliente_id, vehiculo_id, " + "fecha_hora, estado, origen, notas, created_at, updated_at " + "FROM cita WHERE empresa_id = ? ORDER BY fecha_hora ASC";

        List<Appointment> result = new ArrayList<>();

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, empresaId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al listar citas para la empresa " + empresaId, e);
        }

        return result;
    }

    @Override
    public Optional<Appointment> findById(Long id) {
        String sql = "SELECT id, empresa_id, cliente_id, vehiculo_id, " + "fecha_hora, estado, origen, notas, created_at, updated_at " + "FROM cita WHERE id = ?";

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar cita con id " + id, e);
        }

        return Optional.empty();
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

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar cita con id " + id, e);
        }
    }

    @Override
    public boolean existsAtDateTime(Long empresaId, Long vehicleId, LocalDateTime dateTime, Long excludeId) {

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM cita " + "WHERE empresa_id = ? " + "AND fecha_hora = ? ");

        if (vehicleId != null) {
            sql.append("AND vehiculo_id = ? ");
        } else {
            sql.append("AND vehiculo_id IS NULL ");
        }

        if (excludeId != null) {
            sql.append("AND id <> ? ");
        }

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            int idx = 1;
            statement.setLong(idx++, empresaId);
            statement.setTimestamp(idx++, Timestamp.valueOf(dateTime));

            if (vehicleId != null) {
                statement.setLong(idx++, vehicleId);
            }

            if (excludeId != null) {
                statement.setLong(idx, excludeId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    long count = rs.getLong(1);
                    return count > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al comprobar existencia de cita en esa fecha/hora", e);
        }

        return false;
    }

    private void insert(Appointment appointment) {
        String sql = "INSERT INTO cita (" + "empresa_id, cliente_id, vehiculo_id, " + "fecha_hora, estado, origen, notas" + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int idx = 1;
            statement.setLong(idx++, appointment.getEmpresaId());
            statement.setLong(idx++, appointment.getCustomerId());

            if (appointment.getVehicleId() != null) {
                statement.setLong(idx++, appointment.getVehicleId());
            } else {
                statement.setNull(idx++, Types.BIGINT);
            }

            statement.setTimestamp(idx++, Timestamp.valueOf(appointment.getDateTime()));
            statement.setString(idx++, mapStatusToDb(appointment.getStatus()));
            statement.setString(idx++, mapOriginToDb(appointment.getOrigin()));
            statement.setString(idx, appointment.getNotes());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    appointment.setId(id);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar cita", e);
        }
    }

    private void update(Appointment appointment) {
        String sql = "UPDATE cita SET " + "empresa_id = ?, " + "cliente_id = ?, " + "vehiculo_id = ?, " + "fecha_hora = ?, " + "estado = ?, " + "origen = ?, " + "notas = ? " + "WHERE id = ?";

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            int idx = 1;
            statement.setLong(idx++, appointment.getEmpresaId());
            statement.setLong(idx++, appointment.getCustomerId());

            if (appointment.getVehicleId() != null) {
                statement.setLong(idx++, appointment.getVehicleId());
            } else {
                statement.setNull(idx++, Types.BIGINT);
            }

            statement.setTimestamp(idx++, Timestamp.valueOf(appointment.getDateTime()));
            statement.setString(idx++, mapStatusToDb(appointment.getStatus()));
            statement.setString(idx++, mapOriginToDb(appointment.getOrigin()));
            statement.setString(idx++, appointment.getNotes());
            statement.setLong(idx, appointment.getId());
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar cita con id " + appointment.getId(), e);
        }
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {

        Appointment appointment = new Appointment();
        appointment.setId(rs.getLong("id"));
        appointment.setEmpresaId(rs.getLong("empresa_id"));
        appointment.setCustomerId(rs.getLong("cliente_id"));
        long vehiculoId = rs.getLong("vehiculo_id");

        if (rs.wasNull()) {
            appointment.setVehicleId(null);
        } else {
            appointment.setVehicleId(vehiculoId);
        }

        Timestamp fechaHoraTs = rs.getTimestamp("fecha_hora");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        appointment.setDateTime(fechaHoraTs != null ? fechaHoraTs.toLocalDateTime() : null);
        String estadoDb = rs.getString("estado");
        String origenDb = rs.getString("origen");
        appointment.setStatus(mapStatusFromDb(estadoDb));
        appointment.setOrigin(mapOriginFromDb(origenDb));
        appointment.setNotes(rs.getString("notas"));
        appointment.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        appointment.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);

        return appointment;
    }

    private String mapStatusToDb(AppointmentStatus status) {
        if (status == null) {
            return "PENDIENTE";
        }
        return switch (status) {
            case PENDING ->
                "PENDIENTE";
            case CONFIRMED ->
                "CONFIRMADA";
            case CANCELLED ->
                "CANCELADA";
            case COMPLETED ->
                "COMPLETADA";
        };
    }

    private AppointmentStatus mapStatusFromDb(String dbValue) {
        if (dbValue == null) {
            return AppointmentStatus.PENDING;
        }
        return switch (dbValue) {
            case "PENDIENTE" ->
                AppointmentStatus.PENDING;
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
}
