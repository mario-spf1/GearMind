package com.gearmind.infrastructure.auth;

import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRepository;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.database.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlUserRepository implements UserRepository {

    private final DataSource dataSource;

    public MySqlUserRepository() {
        this(DataSourceFactory.getDataSource());
    }

    public MySqlUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        String sql = """
                SELECT id, empresa_id, nombre, email, password_hash, rol, activo
                FROM usuario
                WHERE email = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando usuario por email", e);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = """
                SELECT id, empresa_id, nombre, email, password_hash, rol, activo
                FROM usuario
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando usuario por id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<User> findByEmpresaId(long empresaId) {
        List<User> result = new ArrayList<>();

        String sql = """
                SELECT id, empresa_id, nombre, email, password_hash, rol, activo
                FROM usuario
                WHERE empresa_id = ? AND activo = 1
                ORDER BY nombre ASC
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, empresaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando usuarios", e);
        }

        return result;
    }

    @Override
    public User create(long empresaId, String nombre, String email, String passwordHash, UserRole rol, boolean activo) {

        String sql = """
                INSERT INTO usuario (empresa_id, nombre, email, password_hash, rol, activo)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, empresaId);
            ps.setString(2, nombre);
            ps.setString(3, email);
            ps.setString(4, passwordHash);
            ps.setString(5, rol.name());
            ps.setBoolean(6, activo);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new User(id, empresaId, nombre, email, passwordHash, rol, activo);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creando usuario", e);
        }

        return new User(0L, empresaId, nombre, email, passwordHash, rol, activo);
    }

    @Override
    public User update(long id,
            long empresaId,
            String nombre,
            String email,
            String passwordHash,
            UserRole rol,
            boolean activo) {

        String sql = """
                UPDATE usuario
                SET nombre = ?, email = ?, password_hash = ?, rol = ?, activo = ?
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, rol.name());
            ps.setBoolean(5, activo);
            ps.setLong(6, id);
            ps.setLong(7, empresaId);

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando usuario", e);
        }

        return new User(id, empresaId, nombre, email, passwordHash, rol, activo);
    }

    @Override
    public void deactivate(long id, long empresaId) {
        String sql = """
                UPDATE usuario
                SET activo = 0
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, empresaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error desactivando usuario", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long empresaId = rs.getLong("empresa_id");
        String nombre = rs.getString("nombre");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        UserRole rol = UserRole.valueOf(rs.getString("rol"));
        boolean activo = rs.getBoolean("activo");

        return new User(id, empresaId, nombre, email, passwordHash, rol, activo);
    }
}
