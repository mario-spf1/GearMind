package com.gearmind.infrastructure.product;

import com.gearmind.domain.product.Product;
import com.gearmind.domain.product.ProductRepository;
import com.gearmind.infrastructure.database.DataSourceFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlProductRepository implements ProductRepository {

    private final DataSource dataSource;

    public MySqlProductRepository() {
        this.dataSource = DataSourceFactory.getDataSource();
    }

    @Override
    public List<Product> findAll() {
        List<Product> result = new ArrayList<>();

        String sql = """
                SELECT id, empresa_id, nombre, descripcion, referencia, categoria, stock, stock_minimo,
                       precio_compra, precio_venta, activo, created_at, updated_at
                FROM producto
                ORDER BY nombre ASC
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error listando productos", e);
        }

        return result;
    }

    @Override
    public List<Product> findByEmpresaId(long empresaId) {
        List<Product> result = new ArrayList<>();

        String sql = """
                SELECT id, empresa_id, nombre, descripcion, referencia, categoria, stock, stock_minimo,
                       precio_compra, precio_venta, activo, created_at, updated_at
                FROM producto
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
            throw new RuntimeException("Error listando productos", e);
        }

        return result;
    }

    @Override
    public Optional<Product> findById(long id) {
        String sql = """
                SELECT id, empresa_id, nombre, descripcion, referencia, categoria, stock, stock_minimo,
                       precio_compra, precio_venta, activo, created_at, updated_at
                FROM producto
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
    public Product create(long empresaId, String nombre, String descripcion, String referencia, String categoria, int stock, int stockMinimo, BigDecimal precioCompra, BigDecimal precioVenta) {
        String sql = """
                INSERT INTO producto
                    (empresa_id, nombre, descripcion, referencia, categoria, stock, stock_minimo, precio_compra, precio_venta, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, empresaId);
            ps.setString(2, nombre);
            ps.setString(3, descripcion);
            ps.setString(4, referencia);
            ps.setString(5, categoria);
            ps.setInt(6, stock);
            ps.setInt(7, stockMinimo);
            ps.setBigDecimal(8, precioCompra);
            ps.setBigDecimal(9, precioVenta);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new Product(id, empresaId, nombre, descripcion, referencia, categoria, stock, stockMinimo, precioCompra, precioVenta, true);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new Product(0L, empresaId, nombre, descripcion, referencia, categoria, stock, stockMinimo, precioCompra, precioVenta, true);
    }

    @Override
    public Product update(long id, long empresaId, String nombre, String descripcion, String referencia, String categoria, int stock, int stockMinimo, BigDecimal precioCompra, BigDecimal precioVenta) {
        String sql = """
                UPDATE producto
                SET nombre = ?, descripcion = ?, referencia = ?, categoria = ?, stock = ?, stock_minimo = ?,
                    precio_compra = ?, precio_venta = ?, updated_at = NOW()
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, descripcion);
            ps.setString(3, referencia);
            ps.setString(4, categoria);
            ps.setInt(5, stock);
            ps.setInt(6, stockMinimo);
            ps.setBigDecimal(7, precioCompra);
            ps.setBigDecimal(8, precioVenta);
            ps.setLong(9, id);
            ps.setLong(10, empresaId);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new Product(id, empresaId, nombre, descripcion, referencia, categoria, stock, stockMinimo, precioCompra, precioVenta, true);
    }

    @Override
    public void deactivate(long productId, long empresaId) {
        String sql = """
                UPDATE producto
                SET activo = 0, updated_at = NOW()
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, productId);
            ps.setLong(2, empresaId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void activate(long productId, long empresaId) {
        String sql = """
                UPDATE producto
                SET activo = 1, updated_at = NOW()
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, productId);
            ps.setLong(2, empresaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error activando producto " + productId, e);
        }
    }

    @Override
    public List<Product> findAllWithEmpresa() {
        List<Product> result = new ArrayList<>();

        String sql = """
            SELECT p.id, p.empresa_id, p.nombre, p.descripcion, p.referencia, p.categoria, p.stock, p.stock_minimo,
                   p.precio_compra, p.precio_venta, p.activo, p.created_at, p.updated_at,
                   e.nombre AS empresa_nombre
            FROM producto p
            JOIN empresa e ON e.id = p.empresa_id
            ORDER BY e.nombre, p.nombre
            """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product product = mapRow(rs);
                product.setEmpresaNombre(rs.getString("empresa_nombre"));
                result.add(product);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error listando productos con empresa", e);
        }

        return result;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setEmpresaId(rs.getLong("empresa_id"));
        product.setNombre(rs.getString("nombre"));
        product.setDescripcion(rs.getString("descripcion"));
        product.setReferencia(rs.getString("referencia"));
        product.setCategoria(rs.getString("categoria"));
        product.setStock(rs.getInt("stock"));
        product.setStockMinimo(rs.getInt("stock_minimo"));
        product.setPrecioCompra(rs.getBigDecimal("precio_compra"));
        product.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        product.setActivo(rs.getBoolean("activo"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            product.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            product.setUpdatedAt(updated.toLocalDateTime());
        }

        return product;
    }
}
