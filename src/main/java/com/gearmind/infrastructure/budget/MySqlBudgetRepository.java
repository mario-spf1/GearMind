package com.gearmind.infrastructure.budget;

import com.gearmind.domain.budget.Budget;
import com.gearmind.domain.budget.BudgetLine;
import com.gearmind.domain.budget.BudgetRepository;
import com.gearmind.domain.budget.BudgetStatus;
import com.gearmind.infrastructure.database.DataSourceFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlBudgetRepository implements BudgetRepository {

    private final DataSource dataSource;

    public MySqlBudgetRepository() {
        this.dataSource = DataSourceFactory.getDataSource();
    }

    @Override
    public List<Budget> findAllWithEmpresa() {
        String sql = """
                SELECT p.id,
                    p.empresa_id,
                    p.cliente_id,
                    p.vehiculo_id,
                    p.reparacion_id,
                    p.fecha,
                    p.estado,
                    p.observaciones,
                    p.total_estimado,
                    p.created_at,
                    p.updated_at,
                    c.nombre AS cliente_nombre,
                    v.matricula AS vehiculo_matricula,
                    v.marca AS vehiculo_marca,
                    v.modelo AS vehiculo_modelo,
                    e.nombre AS empresa_nombre,
                    r.descripcion AS reparacion_descripcion
                FROM presupuesto p
                JOIN cliente c ON c.id = p.cliente_id
                JOIN vehiculo v ON v.id = p.vehiculo_id
                JOIN empresa e ON e.id = p.empresa_id
                LEFT JOIN reparacion r ON r.id = p.reparacion_id
                ORDER BY p.fecha DESC
                """;

        List<Budget> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar presupuestos", e);
        }

        return result;
    }

    @Override
    public List<Budget> findByEmpresaId(long empresaId) {
        String sql = """
                SELECT p.id,
                    p.empresa_id,
                    p.cliente_id,
                    p.vehiculo_id,
                    p.reparacion_id,
                    p.fecha,
                    p.estado,
                    p.observaciones,
                    p.total_estimado,
                    p.created_at,
                    p.updated_at,
                    c.nombre AS cliente_nombre,
                    v.matricula AS vehiculo_matricula,
                    v.marca AS vehiculo_marca,
                    v.modelo AS vehiculo_modelo,
                    e.nombre AS empresa_nombre,
                    r.descripcion AS reparacion_descripcion
                FROM presupuesto p
                JOIN cliente c ON c.id = p.cliente_id
                JOIN vehiculo v ON v.id = p.vehiculo_id
                JOIN empresa e ON e.id = p.empresa_id
                LEFT JOIN reparacion r ON r.id = p.reparacion_id
                WHERE p.empresa_id = ?
                ORDER BY p.fecha DESC
                """;

        List<Budget> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar presupuestos", e);
        }

        return result;
    }

    @Override
    public Optional<Budget> findById(long id) {
        String sql = """
                SELECT p.id,
                    p.empresa_id,
                    p.cliente_id,
                    p.vehiculo_id,
                    p.reparacion_id,
                    p.fecha,
                    p.estado,
                    p.observaciones,
                    p.total_estimado,
                    p.created_at,
                    p.updated_at,
                    c.nombre AS cliente_nombre,
                    v.matricula AS vehiculo_matricula,
                    v.marca AS vehiculo_marca,
                    v.modelo AS vehiculo_modelo,
                    e.nombre AS empresa_nombre,
                    r.descripcion AS reparacion_descripcion
                FROM presupuesto p
                JOIN cliente c ON c.id = p.cliente_id
                JOIN vehiculo v ON v.id = p.vehiculo_id
                JOIN empresa e ON e.id = p.empresa_id
                LEFT JOIN reparacion r ON r.id = p.reparacion_id
                WHERE p.id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar presupuesto", e);
        }

        return Optional.empty();
    }

    @Override
    public List<BudgetLine> findLinesByBudgetId(long budgetId) {
        String sql = """
                SELECT id, presupuesto_id, producto_id, descripcion, cantidad, precio, total
                FROM presupuesto_linea
                WHERE presupuesto_id = ?
                ORDER BY id ASC
                """;

        List<BudgetLine> lines = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, budgetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BudgetLine line = new BudgetLine();
                    line.setId(rs.getLong("id"));
                    line.setPresupuestoId(rs.getLong("presupuesto_id"));
                    long productoId = rs.getLong("producto_id");
                    if (!rs.wasNull()) {
                        line.setProductoId(productoId);
                    }
                    line.setDescripcion(rs.getString("descripcion"));
                    line.setCantidad(rs.getBigDecimal("cantidad"));
                    line.setPrecio(rs.getBigDecimal("precio"));
                    line.setTotal(rs.getBigDecimal("total"));
                    lines.add(line);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar líneas del presupuesto", e);
        }

        return lines;
    }

    @Override
    public Budget save(Budget budget, List<BudgetLine> lines) {
        if (budget.getId() == null) {
            long id = insertBudget(budget);
            budget.setId(id);
        } else {
            updateBudget(budget);
            deleteLines(budget.getId());
        }

        insertLines(budget.getId(), lines);
        return budget;
    }

    private long insertBudget(Budget budget) {
        String sql = """
                INSERT INTO presupuesto
                    (empresa_id, cliente_id, vehiculo_id, reparacion_id, fecha, estado, observaciones, total_estimado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setLong(i++, budget.getEmpresaId());
            ps.setLong(i++, budget.getClienteId());
            ps.setLong(i++, budget.getVehiculoId());
            if (budget.getReparacionId() != null) {
                ps.setLong(i++, budget.getReparacionId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }
            ps.setTimestamp(i++, Timestamp.valueOf(budget.getFecha() != null ? budget.getFecha() : LocalDateTime.now()));
            ps.setString(i++, mapStatusToDb(budget.getEstado()));
            ps.setString(i++, budget.getObservaciones());
            ps.setBigDecimal(i++, budget.getTotalEstimado() != null ? budget.getTotalEstimado() : BigDecimal.ZERO);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear presupuesto", e);
        }

        return 0L;
    }

    private void updateBudget(Budget budget) {
        String sql = """
                UPDATE presupuesto
                SET cliente_id = ?, vehiculo_id = ?, reparacion_id = ?, estado = ?, observaciones = ?,
                    total_estimado = ?, updated_at = NOW()
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, budget.getClienteId());
            ps.setLong(i++, budget.getVehiculoId());
            if (budget.getReparacionId() != null) {
                ps.setLong(i++, budget.getReparacionId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }
            ps.setString(i++, mapStatusToDb(budget.getEstado()));
            ps.setString(i++, budget.getObservaciones());
            ps.setBigDecimal(i++, budget.getTotalEstimado() != null ? budget.getTotalEstimado() : BigDecimal.ZERO);
            ps.setLong(i++, budget.getId());
            ps.setLong(i++, budget.getEmpresaId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar presupuesto", e);
        }
    }

    private void deleteLines(long budgetId) {
        String sql = "DELETE FROM presupuesto_linea WHERE presupuesto_id = ?";
        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, budgetId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al limpiar líneas del presupuesto", e);
        }
    }

    private void insertLines(long budgetId, List<BudgetLine> lines) {
        String sql = """
                INSERT INTO presupuesto_linea
                    (presupuesto_id, producto_id, descripcion, cantidad, precio, total)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            for (BudgetLine line : lines) {
                int i = 1;
                ps.setLong(i++, budgetId);
                if (line.getProductoId() != null) {
                    ps.setLong(i++, line.getProductoId());
                } else {
                    ps.setNull(i++, Types.BIGINT);
                }
                ps.setString(i++, line.getDescripcion());
                ps.setBigDecimal(i++, line.getCantidad());
                ps.setBigDecimal(i++, line.getPrecio());
                ps.setBigDecimal(i++, line.getTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar líneas del presupuesto", e);
        }
    }

    private Budget mapRow(ResultSet rs) throws SQLException {
        Budget budget = new Budget();
        budget.setId(rs.getLong("id"));
        budget.setEmpresaId(rs.getLong("empresa_id"));
        budget.setClienteId(rs.getLong("cliente_id"));
        budget.setVehiculoId(rs.getLong("vehiculo_id"));
        long reparacionId = rs.getLong("reparacion_id");
        if (!rs.wasNull()) {
            budget.setReparacionId(reparacionId);
        }
        Timestamp fecha = rs.getTimestamp("fecha");
        if (fecha != null) {
            budget.setFecha(fecha.toLocalDateTime());
        }
        budget.setEstado(mapStatusFromDb(rs.getString("estado")));
        budget.setObservaciones(rs.getString("observaciones"));
        budget.setTotalEstimado(rs.getBigDecimal("total_estimado"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            budget.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            budget.setUpdatedAt(updated.toLocalDateTime());
        }
        budget.setClienteNombre(rs.getString("cliente_nombre"));
        budget.setEmpresaNombre(rs.getString("empresa_nombre"));
        String vehiculoLabel = String.format("%s %s", safe(rs.getString("vehiculo_marca")), safe(rs.getString("vehiculo_modelo"))).trim();
        String matricula = rs.getString("vehiculo_matricula");
        if (matricula != null && !matricula.isBlank()) {
            vehiculoLabel = vehiculoLabel + " - " + matricula;
        }
        budget.setVehiculoEtiqueta(vehiculoLabel.trim());
        budget.setReparacionDescripcion(rs.getString("reparacion_descripcion"));
        return budget;
    }

    private BudgetStatus mapStatusFromDb(String value) {
        if (value == null) {
            return BudgetStatus.BORRADOR;
        }
        return BudgetStatus.valueOf(value);
    }

    private String mapStatusToDb(BudgetStatus status) {
        if (status == null) {
            return BudgetStatus.BORRADOR.name();
        }
        return status.name();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
