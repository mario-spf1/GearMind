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
    public List<Customer> findAll() {
        List<Customer> result = new ArrayList<>();

        String sql = """
                SELECT id, empresa_id, nombre, dni, email, telefono, notas, activo
                FROM cliente
                ORDER BY nombre ASC
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error listando todos los clientes", e);
        }

        return result;
    }

    @Override
    public List<Customer> findByEmpresaId(long empresaId) {
        List<Customer> result = new ArrayList<>();

        String sql = """
                SELECT id, empresa_id, nombre, dni, email, telefono, notas, activo
                FROM cliente
                WHERE empresa_id = ?
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
            throw new RuntimeException("Error listando clientes", e);
        }

        return result;
    }

    @Override
    public Optional<Customer> findById(long id) {
        String sql = """
                SELECT id, empresa_id, nombre, dni, email, telefono, notas, activo
                FROM cliente
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
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Customer create(long empresaId, String nombre, String dni, String email, String telefono, String notas) {
        String sql = """
                INSERT INTO cliente (empresa_id, nombre, dni, email, telefono, notas, activo)
                VALUES (?, ?, ?, ?, ?, ?, 1)
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, empresaId);
            ps.setString(2, nombre);
            ps.setString(3, dni);
            ps.setString(4, email);
            ps.setString(5, telefono);
            ps.setString(6, notas);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new Customer(id, empresaId, nombre, dni, email, telefono, notas, true);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new Customer(0L, empresaId, nombre, dni, email, telefono, notas, true);
    }

    @Override
    public Customer update(long id, long empresaId, String nombre, String dni, String email, String telefono, String notas) {
        String sql = """
                UPDATE cliente
                SET nombre = ?, dni = ?, email = ?, telefono = ?, notas = ?
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, dni);
            ps.setString(3, email);
            ps.setString(4, telefono);
            ps.setString(5, notas);
            ps.setLong(6, id);
            ps.setLong(7, empresaId);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new Customer(id, empresaId, nombre, dni, email, telefono, notas, true);
    }

    @Override
    public void deactivate(long id, long empresaId) {
        String sql = """
                UPDATE cliente
                SET activo = 0
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, empresaId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void activate(long customerId, long empresaId) {
        String sql = """
                UPDATE cliente
                SET activo = 1, updated_at = NOW()
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);
            ps.setLong(2, empresaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error activando cliente " + customerId, e);
        }
    }

    @Override
    public void delete(long customerId, long empresaId) {
        String sql = """
                DELETE FROM cliente
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            ps.setLong(2, empresaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando cliente " + customerId, e);
        }
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("nombre"), rs.getString("dni"), rs.getString("email"), rs.getString("telefono"), rs.getString("notas"), rs.getBoolean("activo"));
    }

    @Override
    public List<Customer> findAllWithEmpresa() {
        List<Customer> result = new ArrayList<>();

        String sql = """
            SELECT c.id,
                c.empresa_id,
                e.nombre AS empresa_nombre,
                c.nombre,
                c.dni,
                c.email,
                c.telefono,
                c.notas,
                c.activo
            FROM cliente c
            JOIN empresa e ON e.id = c.empresa_id
            ORDER BY e.nombre, c.nombre
            """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Customer c = new Customer(rs.getLong("id"), rs.getLong("empresa_id"), rs.getString("nombre"), rs.getString("dni"), rs.getString("email"), rs.getString("telefono"), rs.getString("notas"), rs.getBoolean("activo"));
                c.setEmpresaNombre(rs.getString("empresa_nombre"));
                result.add(c);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error listando clientes con empresa", e);
        }

        return result;
    }

    @Override
    public Optional<Customer> findByDni(long empresaId, String dni) {
        String sql = """
                SELECT id, empresa_id, nombre, dni, email, telefono, notas, activo
                FROM cliente
                WHERE empresa_id = ? AND dni = ?
                LIMIT 1
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, dni);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando cliente por DNI", e);
        }

        return Optional.empty();
    }
}
