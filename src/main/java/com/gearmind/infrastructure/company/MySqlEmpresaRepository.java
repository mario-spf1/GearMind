package com.gearmind.infrastructure.company;

import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.company.EmpresaRepository;
import com.gearmind.infrastructure.database.DataSourceFactory;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlEmpresaRepository implements EmpresaRepository {

    private final DataSource dataSource;

    public MySqlEmpresaRepository() {
        this.dataSource = DataSourceFactory.getDataSource();
    }

    @Override
    public List<Empresa> findAll() {
        String sql = """
                SELECT id, nombre, cif, telefono, email, direccion,
                    ciudad, provincia, cp, activa
                FROM empresa
                ORDER BY id
                """;

        List<Empresa> empresas = new ArrayList<>();

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                empresas.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar empresas", e);
        }

        return empresas;
    }

    @Override
    public Optional<Empresa> findById(long id) {
        String sql = """
                SELECT id, nombre, cif, telefono, email, direccion,
                    ciudad, provincia, cp, activa
                FROM empresa
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
            throw new RuntimeException("Error al buscar empresa por id", e);
        }

        return Optional.empty();
    }

    @Override
    public Empresa save(Empresa empresa) {
        if (empresa.isNew()) {
            return insert(empresa);
        } else {
            return update(empresa);
        }
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM empresa WHERE id = ?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar empresa", e);
        }
    }

    private Empresa insert(Empresa empresa) {
        String sql = """
                INSERT INTO empresa
                    (nombre, cif, telefono, email, direccion,
                    ciudad, provincia, cp, activa)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            fillStatement(ps, empresa);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new Empresa(id, empresa.getNombre(), empresa.getCif(), empresa.getTelefono(),
                            empresa.getEmail(), empresa.getDireccion(), empresa.getCiudad(), empresa.getProvincia(),
                            empresa.getCp(), empresa.isActiva());
                } else {
                    throw new RuntimeException("No se pudo obtener el ID generado de empresa");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar empresa", e);
        }
    }

    private Empresa update(Empresa empresa) {
        String sql = """
                UPDATE empresa
                SET nombre = ?, cif = ?, telefono = ?, email = ?,
                    direccion = ?, ciudad = ?, provincia = ?, cp = ?, activa = ?
                WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            fillStatement(ps, empresa);
            ps.setLong(10, empresa.getId());

            ps.executeUpdate();

            return empresa;

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar empresa", e);
        }
    }

    private void fillStatement(PreparedStatement ps, Empresa e) throws SQLException {
        ps.setString(1, e.getNombre());
        ps.setString(2, e.getCif());
        ps.setString(3, e.getTelefono());
        ps.setString(4, e.getEmail());
        ps.setString(5, e.getDireccion());
        ps.setString(6, e.getCiudad());
        ps.setString(7, e.getProvincia());
        ps.setString(8, e.getCp());
        ps.setBoolean(9, e.isActiva());
    }

    private Empresa mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String nombre = rs.getString("nombre");
        String cif = rs.getString("cif");
        String telefono = rs.getString("telefono");
        String email = rs.getString("email");
        String direccion = rs.getString("direccion");
        String ciudad = rs.getString("ciudad");
        String provincia = rs.getString("provincia");
        String cp = rs.getString("cp");
        boolean activa = rs.getBoolean("activa");

        return new Empresa(id, nombre, cif, telefono, email, direccion, ciudad, provincia, cp, activa);
    }
}
