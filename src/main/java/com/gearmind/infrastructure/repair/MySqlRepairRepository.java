package com.gearmind.infrastructure.repair;

import com.gearmind.domain.repair.Repair;
import com.gearmind.domain.repair.RepairRepository;
import com.gearmind.domain.repair.RepairStatus;
import com.gearmind.infrastructure.database.DataSourceFactory;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlRepairRepository implements RepairRepository {

    private final DataSource dataSource;

    public MySqlRepairRepository() {
        this.dataSource = DataSourceFactory.getDataSource();
    }

    @Override
    public List<Repair> findByEmpresa(Long empresaId) {
        String sql = """
                SELECT r.id,
                    r.empresa_id,
                    r.cita_id,
                    r.cliente_id,
                    r.vehiculo_id,
                    r.descripcion,
                    r.estado,
                    r.importe_estimado,
                    r.importe_final,
                    r.created_at,
                    r.updated_at,
                    c.nombre AS cliente_nombre,
                    v.matricula AS vehiculo_matricula,
                    v.marca AS vehiculo_marca,
                    v.modelo AS vehiculo_modelo,
                    e.nombre AS empresa_nombre
                FROM reparacion r
                JOIN cliente c ON c.id = r.cliente_id
                JOIN vehiculo v ON v.id = r.vehiculo_id
                JOIN empresa e ON e.id = r.empresa_id
                WHERE r.empresa_id = ?
                ORDER BY r.created_at DESC
                """;

        List<Repair> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar reparaciones", e);
        }

        return result;
    }

    @Override
    public List<Repair> findAllWithEmpresa() {
        String sql = """
                SELECT r.id,
                    r.empresa_id,
                    r.cita_id,
                    r.cliente_id,
                    r.vehiculo_id,
                    r.descripcion,
                    r.estado,
                    r.importe_estimado,
                    r.importe_final,
                    r.created_at,
                    r.updated_at,
                    c.nombre AS cliente_nombre,
                    v.matricula AS vehiculo_matricula,
                    v.marca AS vehiculo_marca,
                    v.modelo AS vehiculo_modelo,
                    e.nombre AS empresa_nombre
                FROM reparacion r
                JOIN cliente c ON c.id = r.cliente_id
                JOIN vehiculo v ON v.id = r.vehiculo_id
                JOIN empresa e ON e.id = r.empresa_id
                ORDER BY e.nombre, r.created_at DESC
                """;

        List<Repair> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar reparaciones", e);
        }

        return result;
    }

    @Override
    public Optional<Repair> findById(Long id) {
        String sql = """
                SELECT r.id,
                    r.empresa_id,
                    r.cita_id,
                    r.cliente_id,
                    r.vehiculo_id,
                    r.descripcion,
                    r.estado,
                    r.importe_estimado,
                    r.importe_final,
                    r.created_at,
                    r.updated_at,
                    c.nombre AS cliente_nombre,
                    v.matricula AS vehiculo_matricula,
                    v.marca AS vehiculo_marca,
                    v.modelo AS vehiculo_modelo,
                    e.nombre AS empresa_nombre
                FROM reparacion r
                JOIN cliente c ON c.id = r.cliente_id
                JOIN vehiculo v ON v.id = r.vehiculo_id
                JOIN empresa e ON e.id = r.empresa_id
                WHERE r.id = ?
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
            throw new RuntimeException("Error al buscar reparación por id", e);
        }
    }

    @Override
    public void save(Repair repair) {
        if (repair.getId() == null) {
            insert(repair);
        } else {
            update(repair);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM reparacion WHERE id = ?";

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al borrar reparación", e);
        }
    }

    private void insert(Repair repair) {
        String sql = """
                INSERT INTO reparacion
                    (empresa_id, cita_id, cliente_id, vehiculo_id,
                     descripcion, estado, importe_estimado, importe_final)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int i = 1;
            ps.setLong(i++, repair.getEmpresaId());

            if (repair.getCitaId() != null) {
                ps.setLong(i++, repair.getCitaId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }

            ps.setLong(i++, repair.getClienteId());
            ps.setLong(i++, repair.getVehiculoId());
            ps.setString(i++, repair.getDescripcion());
            ps.setString(i++, mapStatusToDb(repair.getEstado()));

            if (repair.getImporteEstimado() != null) {
                ps.setBigDecimal(i++, repair.getImporteEstimado());
            } else {
                ps.setNull(i++, Types.DECIMAL);
            }

            if (repair.getImporteFinal() != null) {
                ps.setBigDecimal(i++, repair.getImporteFinal());
            } else {
                ps.setNull(i, Types.DECIMAL);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    repair.setId(rs.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al crear reparación", e);
        }
    }

    private void update(Repair repair) {
        String sql = """
                UPDATE reparacion
                SET empresa_id = ?,
                    cita_id = ?,
                    cliente_id = ?,
                    vehiculo_id = ?,
                    descripcion = ?,
                    estado = ?,
                    importe_estimado = ?,
                    importe_final = ?
                WHERE id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            int i = 1;
            ps.setLong(i++, repair.getEmpresaId());

            if (repair.getCitaId() != null) {
                ps.setLong(i++, repair.getCitaId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }

            ps.setLong(i++, repair.getClienteId());
            ps.setLong(i++, repair.getVehiculoId());
            ps.setString(i++, repair.getDescripcion());
            ps.setString(i++, mapStatusToDb(repair.getEstado()));

            if (repair.getImporteEstimado() != null) {
                ps.setBigDecimal(i++, repair.getImporteEstimado());
            } else {
                ps.setNull(i++, Types.DECIMAL);
            }

            if (repair.getImporteFinal() != null) {
                ps.setBigDecimal(i++, repair.getImporteFinal());
            } else {
                ps.setNull(i++, Types.DECIMAL);
            }

            ps.setLong(i, repair.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar reparación", e);
        }
    }

    private Repair mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long empresaId = rs.getLong("empresa_id");
        Long citaId = rs.getObject("cita_id") != null ? rs.getLong("cita_id") : null;
        Long clienteId = rs.getLong("cliente_id");
        Long vehiculoId = rs.getLong("vehiculo_id");
        String descripcion = rs.getString("descripcion");
        RepairStatus estado = mapStatusFromDb(rs.getString("estado"));
        BigDecimal importeEstimado = rs.getBigDecimal("importe_estimado");
        BigDecimal importeFinal = rs.getBigDecimal("importe_final");
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        Repair repair = new Repair(id, empresaId, citaId, clienteId, vehiculoId, descripcion, estado, importeEstimado, importeFinal, created != null ? created.toLocalDateTime() : null, updated != null ? updated.toLocalDateTime() : null);
        String clienteNombre = rs.getString("cliente_nombre");
        String matricula = rs.getString("vehiculo_matricula");
        String marca = rs.getString("vehiculo_marca");
        String modelo = rs.getString("vehiculo_modelo");
        String empresaNombre = rs.getString("empresa_nombre");
        repair.setClienteNombre(clienteNombre);
        repair.setEmpresaNombre(empresaNombre);
        repair.setVehiculoEtiqueta(buildVehicleLabel(matricula, marca, modelo));
        return repair;
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

    private RepairStatus mapStatusFromDb(String raw) {
        if (raw == null) {
            return RepairStatus.ABIERTA;
        }
        return RepairStatus.valueOf(raw);
    }

    private String mapStatusToDb(RepairStatus status) {
        if (status == null) {
            return RepairStatus.ABIERTA.name();
        }
        return status.name();
    }
}
