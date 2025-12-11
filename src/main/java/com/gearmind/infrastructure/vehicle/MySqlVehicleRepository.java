package com.gearmind.infrastructure.vehicle;

import com.gearmind.common.exception.InfrastructureException;
import com.gearmind.domain.vehicle.Vehicle;
import com.gearmind.domain.vehicle.VehicleRepository;
import com.gearmind.infrastructure.database.DataSourceFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlVehicleRepository implements VehicleRepository {

    @Override
    public List<Vehicle> findByEmpresaId(Long empresaId) {
        String sql = """
            SELECT v.id, v.empresa_id, v.cliente_id,
                   v.matricula, v.marca, v.modelo, v.year, v.vin,
                   v.created_at, v.updated_at,
                   c.nombre AS cliente_nombre
            FROM vehiculo v
            JOIN cliente c ON c.id = v.cliente_id
            WHERE v.empresa_id = ?
            ORDER BY v.matricula
            """;

        try (Connection con = DataSourceFactory.getDataSource().getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, empresaId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Vehicle> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Error al listar vehículos", e);
        }
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        String sql = """
            SELECT v.id, v.empresa_id, v.cliente_id,
                   v.matricula, v.marca, v.modelo, v.year, v.vin,
                   v.created_at, v.updated_at,
                   c.nombre AS cliente_nombre
            FROM vehiculo v
            JOIN cliente c ON c.id = v.cliente_id
            WHERE v.id = ?
            """;

        try (Connection con = DataSourceFactory.getDataSource().getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Error al buscar vehículo", e);
        }
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        if (vehicle.getId() == null) {
            return insert(vehicle);
        } else {
            return update(vehicle);
        }
    }

    private Vehicle insert(Vehicle vehicle) {
        String sql = """
            INSERT INTO vehiculo
                (empresa_id, cliente_id, matricula, marca, modelo, year, vin)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection con = DataSourceFactory.getDataSource().getConnection(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, vehicle.getEmpresaId());
            ps.setLong(2, vehicle.getClienteId());
            ps.setString(3, vehicle.getMatricula());
            ps.setString(4, vehicle.getMarca());
            ps.setString(5, vehicle.getModelo());
            if (vehicle.getYear() != null) {
                ps.setInt(6, vehicle.getYear());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setString(7, vehicle.getVin());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    vehicle.setId(rs.getLong(1));
                }
            }

            return vehicle;

        } catch (SQLException e) {
            throw new InfrastructureException("Error al crear vehículo", e);
        }
    }

    private Vehicle update(Vehicle vehicle) {
        String sql = """
            UPDATE vehiculo
            SET cliente_id = ?, matricula = ?, marca = ?, modelo = ?, year = ?, vin = ?
            WHERE id = ?
            """;

        try (Connection con = DataSourceFactory.getDataSource().getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, vehicle.getClienteId());
            ps.setString(2, vehicle.getMatricula());
            ps.setString(3, vehicle.getMarca());
            ps.setString(4, vehicle.getModelo());
            if (vehicle.getYear() != null) {
                ps.setInt(5, vehicle.getYear());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, vehicle.getVin());
            ps.setLong(7, vehicle.getId());

            ps.executeUpdate();
            return vehicle;

        } catch (SQLException e) {
            throw new InfrastructureException("Error al actualizar vehículo", e);
        }
    }

    @Override
    public boolean existsMatriculaInEmpresa(Long empresaId, String matricula, Long excludeId) {
        String sql = """
            SELECT COUNT(*) 
            FROM vehiculo 
            WHERE empresa_id = ? 
              AND UPPER(matricula) = UPPER(?) 
              AND (? IS NULL OR id <> ?)
            """;

        try (Connection con = DataSourceFactory.getDataSource().getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, empresaId);
            ps.setString(2, matricula);
            if (excludeId == null) {
                ps.setNull(3, Types.BIGINT);
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(3, excludeId);
                ps.setLong(4, excludeId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Error al comprobar matrícula de vehículo", e);
        }
    }

    private Vehicle mapRow(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setId(rs.getLong("id"));
        v.setEmpresaId(rs.getLong("empresa_id"));
        v.setClienteId(rs.getLong("cliente_id"));
        v.setMatricula(rs.getString("matricula"));
        v.setMarca(rs.getString("marca"));
        v.setModelo(rs.getString("modelo"));

        int year = rs.getInt("year");
        if (!rs.wasNull()) {
            v.setYear(year);
        }

        v.setVin(rs.getString("vin"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            v.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            v.setUpdatedAt(updated.toLocalDateTime());
        }

        v.setClienteNombre(rs.getString("cliente_nombre"));
        return v;
    }
}
