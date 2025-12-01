package com.gearmind.infrastructure.customer;

import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import com.gearmind.infrastructure.database.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
                       telefono
                FROM cliente
                WHERE empresa_id = ?
                ORDER BY nombre ASC
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, empresaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Customer c = new Customer(
                            rs.getLong("id"),
                            rs.getLong("empresa_id"),
                            rs.getString("nombre"),
                            rs.getString("email"),
                            rs.getString("telefono")
                    );
                    result.add(c);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }
}
