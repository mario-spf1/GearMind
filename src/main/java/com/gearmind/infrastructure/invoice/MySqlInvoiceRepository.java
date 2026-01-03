package com.gearmind.infrastructure.invoice;

import com.gearmind.domain.invoice.Invoice;
import com.gearmind.domain.invoice.InvoiceLine;
import com.gearmind.domain.invoice.InvoiceRepository;
import com.gearmind.domain.invoice.InvoiceStatus;
import com.gearmind.infrastructure.database.DataSourceFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlInvoiceRepository implements InvoiceRepository {

    private final DataSource dataSource;

    public MySqlInvoiceRepository() {
        this.dataSource = DataSourceFactory.getDataSource();
    }

    @Override
    public List<Invoice> findAllWithEmpresa() {
        String sql = """
                SELECT f.id,
                    f.empresa_id,
                    f.cliente_id,
                    f.vehiculo_id,
                    f.presupuesto_id,
                    f.numero,
                    f.fecha,
                    f.estado,
                    f.subtotal,
                    f.iva,
                    f.total,
                    f.observaciones,
                    f.created_at,
                    f.updated_at,
                    c.nombre AS cliente_nombre,
                    v.matricula AS vehiculo_matricula,
                    v.marca AS vehiculo_marca,
                    v.modelo AS vehiculo_modelo,
                    e.nombre AS empresa_nombre
                FROM factura f
                JOIN cliente c ON c.id = f.cliente_id
                JOIN vehiculo v ON v.id = f.vehiculo_id
                JOIN empresa e ON e.id = f.empresa_id
                ORDER BY f.fecha DESC
                """;

        List<Invoice> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar facturas", e);
        }

        return result;
    }

    @Override
    public List<Invoice> findByEmpresaId(long empresaId) {
        String sql = """
                SELECT f.id,
                    f.empresa_id,
                    f.cliente_id,
                    f.vehiculo_id,
                    f.presupuesto_id,
                    f.numero,
                    f.fecha,
                    f.estado,
                    f.subtotal,
                    f.iva,
                    f.total,
                    f.observaciones,
                    f.created_at,
                    f.updated_at,
                    c.nombre AS cliente_nombre,
                    v.matricula AS vehiculo_matricula,
                    v.marca AS vehiculo_marca,
                    v.modelo AS vehiculo_modelo,
                    e.nombre AS empresa_nombre
                FROM factura f
                JOIN cliente c ON c.id = f.cliente_id
                JOIN vehiculo v ON v.id = f.vehiculo_id
                JOIN empresa e ON e.id = f.empresa_id
                WHERE f.empresa_id = ?
                ORDER BY f.fecha DESC
                """;

        List<Invoice> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar facturas", e);
        }

        return result;
    }

    @Override
    public Optional<Invoice> findById(long id) {
        String sql = """
                SELECT f.id,
                    f.empresa_id,
                    f.cliente_id,
                    f.vehiculo_id,
                    f.presupuesto_id,
                    f.numero,
                    f.fecha,
                    f.estado,
                    f.subtotal,
                    f.iva,
                    f.total,
                    f.observaciones,
                    f.created_at,
                    f.updated_at,
                    c.nombre AS cliente_nombre,
                    v.matricula AS vehiculo_matricula,
                    v.marca AS vehiculo_marca,
                    v.modelo AS vehiculo_modelo,
                    e.nombre AS empresa_nombre
                FROM factura f
                JOIN cliente c ON c.id = f.cliente_id
                JOIN vehiculo v ON v.id = f.vehiculo_id
                JOIN empresa e ON e.id = f.empresa_id
                WHERE f.id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar factura", e);
        }

        return Optional.empty();
    }

    @Override
    public List<InvoiceLine> findLinesByInvoiceId(long invoiceId) {
        String sql = """
                SELECT id, factura_id, producto_id, descripcion, cantidad, precio, total
                FROM factura_linea
                WHERE factura_id = ?
                ORDER BY id ASC
                """;

        List<InvoiceLine> lines = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceLine line = new InvoiceLine();
                    line.setId(rs.getLong("id"));
                    line.setFacturaId(rs.getLong("factura_id"));
                    long productoId = rs.getLong("producto_id");
                    if (!rs.wasNull()) {
                        line.setProductoId(productoId);
                    }
                    line.setDescripcion(rs.getString("descripcion"));
                    line.setCantidad(rs.getBigDecimal("cantidad"));
                    line.setPrecio(rs.getBigDecimal("precio"));
                    line.setTotal(rs.getBigDecimal("total"));
                    lines.add(line);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al cargar líneas de la factura", e);
        }

        return lines;
    }

    @Override
    public Invoice save(Invoice invoice, List<InvoiceLine> lines) {
        if (invoice.getId() == null) {
            long id = insertInvoice(invoice);
            invoice.setId(id);
        } else {
            updateInvoice(invoice);
            deleteLines(invoice.getId());
        }

        insertLines(invoice.getId(), lines);
        return invoice;
    }

    private long insertInvoice(Invoice invoice) {
        String sql = """
                INSERT INTO factura
                    (empresa_id, cliente_id, vehiculo_id, presupuesto_id, numero, fecha, estado, subtotal, iva, total, observaciones)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setLong(i++, invoice.getEmpresaId());
            ps.setLong(i++, invoice.getClienteId());
            ps.setLong(i++, invoice.getVehiculoId());
            if (invoice.getPresupuestoId() != null) {
                ps.setLong(i++, invoice.getPresupuestoId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }
            ps.setString(i++, invoice.getNumero());
            ps.setTimestamp(i++, Timestamp.valueOf(invoice.getFecha() != null ? invoice.getFecha() : LocalDateTime.now()));
            ps.setString(i++, mapStatusToDb(invoice.getEstado()));
            ps.setBigDecimal(i++, invoice.getSubtotal() != null ? invoice.getSubtotal() : BigDecimal.ZERO);
            ps.setBigDecimal(i++, invoice.getIva() != null ? invoice.getIva() : BigDecimal.ZERO);
            ps.setBigDecimal(i++, invoice.getTotal() != null ? invoice.getTotal() : BigDecimal.ZERO);
            ps.setString(i++, invoice.getObservaciones());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear factura", e);
        }

        return 0L;
    }

    private void updateInvoice(Invoice invoice) {
        String sql = """
                UPDATE factura
                SET cliente_id = ?, vehiculo_id = ?, presupuesto_id = ?, numero = ?, estado = ?,
                    subtotal = ?, iva = ?, total = ?, observaciones = ?, updated_at = NOW()
                WHERE id = ? AND empresa_id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, invoice.getClienteId());
            ps.setLong(i++, invoice.getVehiculoId());
            if (invoice.getPresupuestoId() != null) {
                ps.setLong(i++, invoice.getPresupuestoId());
            } else {
                ps.setNull(i++, Types.BIGINT);
            }
            ps.setString(i++, invoice.getNumero());
            ps.setString(i++, mapStatusToDb(invoice.getEstado()));
            ps.setBigDecimal(i++, invoice.getSubtotal() != null ? invoice.getSubtotal() : BigDecimal.ZERO);
            ps.setBigDecimal(i++, invoice.getIva() != null ? invoice.getIva() : BigDecimal.ZERO);
            ps.setBigDecimal(i++, invoice.getTotal() != null ? invoice.getTotal() : BigDecimal.ZERO);
            ps.setString(i++, invoice.getObservaciones());
            ps.setLong(i++, invoice.getId());
            ps.setLong(i++, invoice.getEmpresaId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar factura", e);
        }
    }

    private void deleteLines(long invoiceId) {
        String sql = "DELETE FROM factura_linea WHERE factura_id = ?";
        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al limpiar líneas de la factura", e);
        }
    }

    private void insertLines(long invoiceId, List<InvoiceLine> lines) {
        String sql = """
                INSERT INTO factura_linea
                    (factura_id, producto_id, descripcion, cantidad, precio, total)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            for (InvoiceLine line : lines) {
                int i = 1;
                ps.setLong(i++, invoiceId);
                if (line.getProductoId() != null) {
                    ps.setLong(i++, line.getProductoId());
                } else {
                    ps.setNull(i++, Types.BIGINT);
                }
                ps.setString(i++, line.getDescripcion());
                ps.setBigDecimal(i++, line.getCantidad());
                ps.setBigDecimal(i++, line.getPrecio());
                ps.setBigDecimal(i++, line.getTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar líneas de la factura", e);
        }
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getLong("id"));
        invoice.setEmpresaId(rs.getLong("empresa_id"));
        invoice.setClienteId(rs.getLong("cliente_id"));
        invoice.setVehiculoId(rs.getLong("vehiculo_id"));
        long presupuestoId = rs.getLong("presupuesto_id");
        if (!rs.wasNull()) {
            invoice.setPresupuestoId(presupuestoId);
        }
        invoice.setNumero(rs.getString("numero"));
        Timestamp fecha = rs.getTimestamp("fecha");
        if (fecha != null) {
            invoice.setFecha(fecha.toLocalDateTime());
        }
        invoice.setEstado(mapStatusFromDb(rs.getString("estado")));
        invoice.setSubtotal(rs.getBigDecimal("subtotal"));
        invoice.setIva(rs.getBigDecimal("iva"));
        invoice.setTotal(rs.getBigDecimal("total"));
        invoice.setObservaciones(rs.getString("observaciones"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            invoice.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            invoice.setUpdatedAt(updated.toLocalDateTime());
        }
        invoice.setClienteNombre(rs.getString("cliente_nombre"));
        invoice.setEmpresaNombre(rs.getString("empresa_nombre"));
        String vehiculoLabel = String.format("%s %s", safe(rs.getString("vehiculo_marca")), safe(rs.getString("vehiculo_modelo"))).trim();
        String matricula = rs.getString("vehiculo_matricula");
        if (matricula != null && !matricula.isBlank()) {
            vehiculoLabel = vehiculoLabel + " - " + matricula;
        }
        invoice.setVehiculoEtiqueta(vehiculoLabel.trim());
        return invoice;
    }

    private InvoiceStatus mapStatusFromDb(String value) {
        if (value == null) {
            return InvoiceStatus.PENDIENTE;
        }
        return InvoiceStatus.valueOf(value);
    }

    private String mapStatusToDb(InvoiceStatus status) {
        if (status == null) {
            return InvoiceStatus.PENDIENTE.name();
        }
        return status.name();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
