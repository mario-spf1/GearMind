package com.gearmind.infrastructure.inventory;

import com.gearmind.domain.inventory.InventoryMovement;
import com.gearmind.domain.inventory.InventoryMovementRepository;
import com.gearmind.domain.inventory.InventoryMovementType;
import com.gearmind.infrastructure.database.DataSourceFactory;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySqlInventoryMovementRepository implements InventoryMovementRepository {

    private final DataSource dataSource;

    public MySqlInventoryMovementRepository() {
        this.dataSource = DataSourceFactory.getDataSource();
    }

    @Override
    public InventoryMovement create(InventoryMovement movement) {
        String sql = """
                INSERT INTO inventario_movimiento
                    (empresa_id, producto_id, tipo, cantidad, referencia, notas, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, movement.getEmpresaId());
            ps.setLong(2, movement.getProductoId());
            ps.setString(3, movement.getTipo().name());
            ps.setInt(4, movement.getCantidad());
            ps.setString(5, movement.getReferencia());
            ps.setString(6, movement.getNotas());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    movement.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registrando movimiento de inventario", e);
        }

        return movement;
    }

    @Override
    public List<InventoryMovement> findByEmpresa(long empresaId) {
        List<InventoryMovement> result = new ArrayList<>();

        String sql = """
                SELECT im.id, im.empresa_id, im.producto_id, im.tipo, im.cantidad, im.referencia, im.notas, im.created_at,
                       p.nombre AS producto_nombre, p.referencia AS producto_referencia
                FROM inventario_movimiento im
                JOIN producto p ON p.id = im.producto_id
                WHERE im.empresa_id = ?
                ORDER BY im.created_at DESC
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando movimientos de inventario", e);
        }

        return result;
    }

    @Override
    public List<InventoryMovement> findAllWithEmpresa() {
        List<InventoryMovement> result = new ArrayList<>();

        String sql = """
                SELECT im.id, im.empresa_id, im.producto_id, im.tipo, im.cantidad, im.referencia, im.notas, im.created_at,
                       p.nombre AS producto_nombre, p.referencia AS producto_referencia,
                       e.nombre AS empresa_nombre
                FROM inventario_movimiento im
                JOIN producto p ON p.id = im.producto_id
                JOIN empresa e ON e.id = im.empresa_id
                ORDER BY e.nombre, im.created_at DESC
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                InventoryMovement movement = mapRow(rs);
                movement.setEmpresaNombre(rs.getString("empresa_nombre"));
                result.add(movement);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando movimientos de inventario con empresa", e);
        }

        return result;
    }

    @Override
    public List<InventoryMovement> findByEmpresaAndReferencia(long empresaId, String referencia) {
        List<InventoryMovement> result = new ArrayList<>();

        String sql = """
                SELECT im.id, im.empresa_id, im.producto_id, im.tipo, im.cantidad, im.referencia, im.notas, im.created_at,
                       p.nombre AS producto_nombre, p.referencia AS producto_referencia
                FROM inventario_movimiento im
                JOIN producto p ON p.id = im.producto_id
                WHERE im.empresa_id = ? AND im.referencia = ?
                ORDER BY im.created_at DESC
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            ps.setString(2, referencia);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando movimientos de inventario por referencia", e);
        }

        return result;
    }

    private InventoryMovement mapRow(ResultSet rs) throws SQLException {
        InventoryMovement movement = new InventoryMovement();
        movement.setId(rs.getLong("id"));
        movement.setEmpresaId(rs.getLong("empresa_id"));
        movement.setProductoId(rs.getLong("producto_id"));
        String tipo = rs.getString("tipo");
        movement.setTipo(tipo != null ? InventoryMovementType.valueOf(tipo) : InventoryMovementType.SALIDA);
        movement.setCantidad(rs.getInt("cantidad"));
        movement.setReferencia(rs.getString("referencia"));
        movement.setNotas(rs.getString("notas"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            movement.setCreatedAt(created.toLocalDateTime());
        }
        movement.setProductoNombre(rs.getString("producto_nombre"));
        movement.setProductoReferencia(rs.getString("producto_referencia"));
        return movement;
    }
}
