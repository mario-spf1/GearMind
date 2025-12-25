package com.gearmind.infrastructure.task;

import com.gearmind.domain.task.Task;
import com.gearmind.domain.task.TaskPriority;
import com.gearmind.domain.task.TaskRepository;
import com.gearmind.domain.task.TaskStatus;
import com.gearmind.infrastructure.database.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlTaskRepository implements TaskRepository {

    private final DataSource dataSource;

    public MySqlTaskRepository() {
        this.dataSource = DataSourceFactory.getDataSource();
    }

    @Override
    public List<Task> findByEmpresa(Long empresaId) {
        String sql = baseSelect() + " WHERE t.empresa_id = ? ORDER BY t.created_at DESC";
        List<Task> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar tareas", e);
        }

        return result;
    }

    @Override
    public List<Task> findAllWithEmpresa() {
        String sql = baseSelect() + " ORDER BY e.nombre, t.created_at DESC";
        List<Task> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar tareas", e);
        }

        return result;
    }

    @Override
    public List<Task> findByEmpleado(Long empresaId, Long empleadoId) {
        String sql = baseSelect() + " WHERE t.empresa_id = ? AND t.asignado_a = ? ORDER BY t.created_at DESC";
        List<Task> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setLong(2, empleadoId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar tareas", e);
        }

        return result;
    }

    @Override
    public Optional<Task> findById(Long id) {
        String sql = baseSelect() + " WHERE t.id = ?";

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar tarea por id", e);
        }
    }

    @Override
    public void save(Task task) {
        if (task.getId() == null) {
            insert(task);
        } else {
            update(task);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM tarea WHERE id = ?";

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al borrar tarea", e);
        }
    }

    private void insert(Task task) {
        String sql = """
                INSERT INTO tarea
                    (empresa_id, orden_trabajo_id, asignado_a, titulo, descripcion, estado, prioridad, fecha_limite)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setLong(i++, task.getEmpresaId());
            if (task.getOrdenTrabajoId() != null) {
                ps.setLong(i++, task.getOrdenTrabajoId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }
            if (task.getAsignadoA() != null) {
                ps.setLong(i++, task.getAsignadoA());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }
            ps.setString(i++, task.getTitulo());
            ps.setString(i++, task.getDescripcion());
            ps.setString(i++, mapStatusToDb(task.getEstado()));
            ps.setString(i++, mapPriorityToDb(task.getPrioridad()));
            if (task.getFechaLimite() != null) {
                ps.setTimestamp(i, Timestamp.valueOf(task.getFechaLimite()));
            } else {
                ps.setNull(i, Types.TIMESTAMP);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    task.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear tarea", e);
        }
    }

    private void update(Task task) {
        String sql = """
                UPDATE tarea
                SET empresa_id = ?,
                    orden_trabajo_id = ?,
                    asignado_a = ?,
                    titulo = ?,
                    descripcion = ?,
                    estado = ?,
                    prioridad = ?,
                    fecha_limite = ?
                WHERE id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, task.getEmpresaId());
            if (task.getOrdenTrabajoId() != null) {
                ps.setLong(i++, task.getOrdenTrabajoId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }
            if (task.getAsignadoA() != null) {
                ps.setLong(i++, task.getAsignadoA());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }
            ps.setString(i++, task.getTitulo());
            ps.setString(i++, task.getDescripcion());
            ps.setString(i++, mapStatusToDb(task.getEstado()));
            ps.setString(i++, mapPriorityToDb(task.getPrioridad()));
            if (task.getFechaLimite() != null) {
                ps.setTimestamp(i++, Timestamp.valueOf(task.getFechaLimite()));
            } else {
                ps.setNull(i++, Types.TIMESTAMP);
            }
            ps.setLong(i, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar tarea", e);
        }
    }

    private String baseSelect() {
        return """
                SELECT t.id,
                    t.empresa_id,
                    t.orden_trabajo_id,
                    t.asignado_a,
                    t.titulo,
                    t.descripcion,
                    t.estado,
                    t.prioridad,
                    t.fecha_limite,
                    t.created_at,
                    t.updated_at,
                    u.nombre AS empleado_nombre,
                    r.descripcion AS reparacion_descripcion,
                    c.nombre AS cliente_nombre,
                    v.matricula AS vehiculo_matricula,
                    v.marca AS vehiculo_marca,
                    v.modelo AS vehiculo_modelo,
                    e.nombre AS empresa_nombre
                FROM tarea t
                LEFT JOIN reparacion r ON r.id = t.orden_trabajo_id
                JOIN empresa e ON e.id = t.empresa_id
                LEFT JOIN usuario u ON u.id = t.asignado_a
                LEFT JOIN cliente c ON c.id = r.cliente_id
                LEFT JOIN vehiculo v ON v.id = r.vehiculo_id
                """;
    }

    private Task mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long empresaId = rs.getLong("empresa_id");
        Long ordenTrabajoId = rs.getObject("orden_trabajo_id") != null ? rs.getLong("orden_trabajo_id") : null;
        Long asignadoA = rs.getObject("asignado_a") != null ? rs.getLong("asignado_a") : null;
        String titulo = rs.getString("titulo");
        String descripcion = rs.getString("descripcion");
        TaskStatus estado = mapStatusFromDb(rs.getString("estado"));
        TaskPriority prioridad = mapPriorityFromDb(rs.getString("prioridad"));
        Timestamp fechaLimite = rs.getTimestamp("fecha_limite");
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");

        Task task = new Task(id, empresaId, ordenTrabajoId, asignadoA, titulo, descripcion, estado, prioridad,
                fechaLimite != null ? fechaLimite.toLocalDateTime() : null,
                created != null ? created.toLocalDateTime() : null,
                updated != null ? updated.toLocalDateTime() : null);

        task.setEmpleadoNombre(rs.getString("empleado_nombre"));
        task.setRepairDescripcion(rs.getString("reparacion_descripcion"));
        task.setClienteNombre(rs.getString("cliente_nombre"));
        task.setEmpresaNombre(rs.getString("empresa_nombre"));
        task.setVehiculoMatricula(rs.getString("vehiculo_matricula"));
        task.setVehiculoEtiqueta(buildVehicleLabel(rs.getString("vehiculo_matricula"), rs.getString("vehiculo_marca"), rs.getString("vehiculo_modelo")));

        return task;
    }

    private String buildVehicleLabel(String matricula, String marca, String modelo) {
        StringBuilder sb = new StringBuilder();
        if (matricula != null && !matricula.isBlank()) {
            sb.append(matricula);
        }
        if (marca != null && !marca.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(marca);
        }
        if (modelo != null && !modelo.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(modelo);
        }
        return sb.toString();
    }

    private TaskStatus mapStatusFromDb(String raw) {
        if (raw == null) {
            return TaskStatus.PENDIENTE;
        }
        return TaskStatus.valueOf(raw);
    }

    private String mapStatusToDb(TaskStatus status) {
        if (status == null) {
            return TaskStatus.PENDIENTE.name();
        }
        return status.name();
    }

    private TaskPriority mapPriorityFromDb(String raw) {
        if (raw == null) {
            return TaskPriority.MEDIA;
        }
        return TaskPriority.valueOf(raw);
    }

    private String mapPriorityToDb(TaskPriority priority) {
        if (priority == null) {
            return TaskPriority.MEDIA.name();
        }
        return priority.name();
    }
}
