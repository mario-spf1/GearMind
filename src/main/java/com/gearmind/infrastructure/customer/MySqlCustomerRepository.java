package com.gearmind.infrastructure.customer;

import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.infrastructure.database.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlCustomerRepository implements CustomerRepository {

    private final DataSource dataSource;

    public MySqlCustomerRepository() {
        this.dataSource = DataSourceFactory.getDataSource();
    }

    @Override
    public List<Customer> findByEmpresaId(long empresaId) {
        List<Customer> result = new ArrayList<>();

        String sql = """
                SELECT id,
                       empresa_id,
                       nombre,
                       email,
                       telefono,
                       notas,
                       activo
                FROM cliente
                WHERE empresa_id = ?
                  AND activo = 1
                ORDER BY nombre ASC
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, empresaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public Optional<Customer> findById(long id) {
        String sql = """
                SELECT id,
                       empresa_id,
                       nombre,
                       email,
                       telefono,
                       notas,
                       activo
                FROM cliente
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Customer create(long empresaId, String nombre, String email, String telefono, String notas) {
        String sql = """
                INSERT INTO cliente (empresa_id, nombre, email, telefono, notas, activo)
                VALUES (?, ?, ?, ?, ?, 1)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, empresaId);
            ps.setString(2, nombre);
            ps.setString(3, email);
            ps.setString(4, telefono);
            ps.setString(5, notas);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new Customer(id, empresaId, nombre, email, telefono, notas, true);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new Customer(0L, empresaId, nombre, email, telefono, notas, true);
    }

    @Override
    public Customer update(long id, long empresaId, String nombre, String email, String telefono, String notas) {
        String sql = """
                UPDATE cliente
                SET nombre  = ?,
                    email   = ?,
                    telefono = ?,
                    notas   = ?
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, email);
            ps.setString(3, telefono);
            ps.setString(4, notas);
            ps.setLong(5, id);
            ps.setLong(6, empresaId);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new Customer(id, empresaId, nombre, email, telefono, notas, true);
    }

    @Override
    public void deactivate(long id, long empresaId) {
        String sql = """
                UPDATE cliente
                SET activo = 0
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, empresaId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getLong("id"),
                rs.getLong("empresa_id"),
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("telefono"),
                rs.getString("notas"),
                rs.getBoolean("activo")
        );
    }
}
