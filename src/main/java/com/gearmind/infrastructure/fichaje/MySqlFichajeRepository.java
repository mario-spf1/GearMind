package com.gearmind.infrastructure.fichaje;

import com.gearmind.domain.fichaje.Fichaje;
import com.gearmind.domain.fichaje.FichajeMovimiento;
import com.gearmind.domain.fichaje.FichajeRepository;
import com.gearmind.infrastructure.database.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlFichajeRepository implements FichajeRepository {

    private final DataSource dataSource;

    public MySqlFichajeRepository() {
        this.dataSource = DataSourceFactory.getDataSource();
    }

    @Override
    public Optional<Fichaje> findLastByUser(Long userId) {
        String sql = """
                SELECT f.id, f.empresa_id, f.user_id, f.movimiento, f.fecha,
                       u.nombre AS usuario_nombre,
                       e.nombre AS empresa_nombre
                FROM fichaje f
                JOIN usuario u ON u.id = f.user_id
                JOIN empresa e ON e.id = f.empresa_id
                WHERE f.user_id = ?
                ORDER BY f.fecha DESC
                LIMIT 1
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el último fichaje", e);
        }
    }

    @Override
    public List<Fichaje> findByFilters(Long empresaId, Long userId) {
        StringBuilder sb = new StringBuilder("""
                SELECT f.id, f.empresa_id, f.user_id, f.movimiento, f.fecha,
                       u.nombre AS usuario_nombre,
                       e.nombre AS empresa_nombre
                FROM fichaje f
                JOIN usuario u ON u.id = f.user_id
                JOIN empresa e ON e.id = f.empresa_id
                """);

        boolean whereAdded = false;
        if (empresaId != null && empresaId > 0) {
            sb.append(" WHERE f.empresa_id = ?");
            whereAdded = true;
        }
        if (userId != null && userId > 0) {
            sb.append(whereAdded ? " AND" : " WHERE");
            sb.append(" f.user_id = ?");
        }
        sb.append(" ORDER BY f.fecha DESC");

        List<Fichaje> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sb.toString())) {
            int index = 1;
            if (empresaId != null && empresaId > 0) {
                ps.setLong(index++, empresaId);
            }
            if (userId != null && userId > 0) {
                ps.setLong(index, userId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar fichajes", e);
        }

        return result;
    }

    @Override
    public List<Fichaje> findByUserAndDate(Long userId, java.time.LocalDate date) {
        String sql = """
                SELECT f.id, f.empresa_id, f.user_id, f.movimiento, f.fecha,
                       u.nombre AS usuario_nombre,
                       e.nombre AS empresa_nombre
                FROM fichaje f
                JOIN usuario u ON u.id = f.user_id
                JOIN empresa e ON e.id = f.empresa_id
                WHERE f.user_id = ?
                  AND f.fecha >= ?
                  AND f.fecha < ?
                ORDER BY f.fecha ASC
                """;

        List<Fichaje> result = new ArrayList<>();
        java.time.LocalDateTime startOfDay = date.atStartOfDay();
        java.time.LocalDateTime endOfDay = startOfDay.plusDays(1);

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(startOfDay));
            ps.setTimestamp(3, Timestamp.valueOf(endOfDay));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar fichajes del día", e);
        }

        return result;
    }

    @Override
    public void save(Fichaje fichaje) {
        String sql = """
                INSERT INTO fichaje (empresa_id, user_id, movimiento, fecha)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, fichaje.getEmpresaId());
            ps.setLong(2, fichaje.getUserId());
            ps.setString(3, fichaje.getMovimiento().name());
            if (fichaje.getFecha() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(fichaje.getFecha()));
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    fichaje.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar fichaje", e);
        }
    }

    private Fichaje mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long empresaId = rs.getLong("empresa_id");
        Long userId = rs.getLong("user_id");
        String movimientoRaw = rs.getString("movimiento");
        Timestamp fecha = rs.getTimestamp("fecha");

        FichajeMovimiento movimiento = movimientoRaw != null ? FichajeMovimiento.valueOf(movimientoRaw) : FichajeMovimiento.ENTRADA;
        Fichaje fichaje = new Fichaje(id, empresaId, userId, movimiento, fecha != null ? fecha.toLocalDateTime() : null);
        fichaje.setUsuarioNombre(rs.getString("usuario_nombre"));
        fichaje.setEmpresaNombre(rs.getString("empresa_nombre"));
        return fichaje;
    }
}
