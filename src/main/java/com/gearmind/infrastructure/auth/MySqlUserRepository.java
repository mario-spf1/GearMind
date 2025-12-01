package com.gearmind.infrastructure.auth;

import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRepository;
import com.gearmind.domain.user.UserRole;
import com.gearmind.infrastructure.database.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.*;
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

                long id = rs.getLong("id");
                long empresaId = rs.getLong("empresa_id");
                String nombre = rs.getString("nombre");
                String emailDb = rs.getString("email");
                String passwordHash = rs.getString("password_hash");
                String rolStr = rs.getString("rol");
                boolean activo = rs.getBoolean("activo");

                UserRole rol = UserRole.valueOf(rolStr);

                User user = new User(id, empresaId, nombre, emailDb, passwordHash, rol, activo);

                return Optional.of(user);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error consultando usuario por email", e);
        }
    }
}
