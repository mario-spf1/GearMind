package com.gearmind.infrastructure.payment;

import com.gearmind.domain.payment.Payment;
import com.gearmind.domain.payment.PaymentInstallment;
import com.gearmind.domain.payment.PaymentInstallmentStatus;
import com.gearmind.domain.payment.PaymentRecord;
import com.gearmind.domain.payment.PaymentRepository;
import com.gearmind.domain.payment.PaymentStatus;
import com.gearmind.domain.payment.PaymentType;
import com.gearmind.infrastructure.database.DataSourceFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlPaymentRepository implements PaymentRepository {

    private final DataSource dataSource;

    public MySqlPaymentRepository() {
        this.dataSource = DataSourceFactory.getDataSource();
    }

    @Override
    public List<Payment> findAllWithEmpresa() {
        String sql = baseSelect() + " ORDER BY p.created_at DESC";
        List<Payment> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapPayment(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar pagos", e);
        }

        return result;
    }

    @Override
    public List<Payment> findByEmpresaId(long empresaId) {
        String sql = baseSelect() + " WHERE p.empresa_id = ? ORDER BY p.created_at DESC";
        List<Payment> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapPayment(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar pagos", e);
        }

        return result;
    }

    @Override
    public Optional<Payment> findById(long pagoId) {
        String sql = baseSelect() + " WHERE p.id = ?";

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, pagoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPayment(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar pago", e);
        }
    }

    @Override
    public Optional<Payment> findByFacturaId(long facturaId) {
        String sql = baseSelect() + " WHERE p.factura_id = ?";

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, facturaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPayment(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar pago por factura", e);
        }
    }

    @Override
    public long createPayment(Payment payment, List<PaymentInstallment> installments, List<PaymentRecord> records) {
        String insertPaymentSql = """
                INSERT INTO pago
                    (empresa_id, factura_id, cliente_id, tipo, estado, total, total_pagado, numero_plazos, interes_porcentaje)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        Connection cn = null;
        try {
            cn = dataSource.getConnection();
            cn.setAutoCommit(false);

            long paymentId;
            try (PreparedStatement ps = cn.prepareStatement(insertPaymentSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                ps.setLong(i++, payment.getEmpresaId());
                ps.setLong(i++, payment.getFacturaId());
                ps.setLong(i++, payment.getClienteId());
                ps.setString(i++, mapTypeToDb(payment.getTipo()));
                ps.setString(i++, mapStatusToDb(payment.getEstado()));
                ps.setBigDecimal(i++, payment.getTotal() != null ? payment.getTotal() : BigDecimal.ZERO);
                ps.setBigDecimal(i++, BigDecimal.ZERO);
                if (payment.getNumeroPlazos() != null) {
                    ps.setInt(i++, payment.getNumeroPlazos());
                } else {
                    ps.setNull(i++, Types.INTEGER);
                }
                ps.setBigDecimal(i++, payment.getInteresPorcentaje() != null ? payment.getInteresPorcentaje() : BigDecimal.ZERO);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        paymentId = keys.getLong(1);
                    } else {
                        throw new SQLException("No se pudo obtener el id del pago");
                    }
                }
            }

            if (installments != null && !installments.isEmpty()) {
                insertInstallments(paymentId, installments, cn);
            }

            if (records != null && !records.isEmpty()) {
                insertRecords(paymentId, records, cn);
                updateTotalPagado(paymentId, cn);
            }

            cn.commit();
            return paymentId;
        } catch (SQLException e) {
            if (cn != null) {
                try {
                    cn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            throw new RuntimeException("Error al crear pago", e);
        } finally {
            if (cn != null) {
                try {
                    cn.setAutoCommit(true);
                    cn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public List<PaymentInstallment> findInstallments(long pagoId) {
        String sql = """
                SELECT id, pago_id, numero, fecha_vencimiento, importe, estado, fecha_pago
                FROM pago_plazo
                WHERE pago_id = ?
                ORDER BY numero
                """;
        List<PaymentInstallment> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, pagoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapInstallment(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar plazos", e);
        }

        return result;
    }

    @Override
    public Optional<PaymentInstallment> findInstallmentById(long installmentId) {
        String sql = """
                SELECT id, pago_id, numero, fecha_vencimiento, importe, estado, fecha_pago
                FROM pago_plazo
                WHERE id = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, installmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapInstallment(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar plazo", e);
        }
    }

    @Override
    public List<PaymentRecord> findRecords(long pagoId) {
        String sql = """
                SELECT id, pago_id, plazo_id, fecha, importe, observaciones
                FROM pago_registro
                WHERE pago_id = ?
                ORDER BY fecha DESC
                """;
        List<PaymentRecord> result = new ArrayList<>();

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, pagoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRecord(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar registros de pago", e);
        }

        return result;
    }

    @Override
    public void registerInstallmentPayment(long pagoId, long installmentId, BigDecimal amount, LocalDateTime fechaPago, String observaciones) {
        String updateInstallmentSql = """
                UPDATE pago_plazo
                SET estado = ?, fecha_pago = ?
                WHERE id = ? AND pago_id = ?
                """;
        String insertRecordSql = """
                INSERT INTO pago_registro
                    (pago_id, plazo_id, fecha, importe, observaciones)
                VALUES (?, ?, ?, ?, ?)
                """;

        Connection cn = null;
        try {
            cn = dataSource.getConnection();
            cn.setAutoCommit(false);

            try (PreparedStatement ps = cn.prepareStatement(updateInstallmentSql)) {
                ps.setString(1, PaymentInstallmentStatus.PAGADO.name());
                ps.setTimestamp(2, Timestamp.valueOf(fechaPago != null ? fechaPago : LocalDateTime.now()));
                ps.setLong(3, installmentId);
                ps.setLong(4, pagoId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = cn.prepareStatement(insertRecordSql)) {
                ps.setLong(1, pagoId);
                ps.setLong(2, installmentId);
                ps.setTimestamp(3, Timestamp.valueOf(fechaPago != null ? fechaPago : LocalDateTime.now()));
                ps.setBigDecimal(4, amount != null ? amount : BigDecimal.ZERO);
                ps.setString(5, observaciones);
                ps.executeUpdate();
            }

            updateTotalPagado(pagoId, cn);
            cn.commit();
        } catch (SQLException e) {
            if (cn != null) {
                try {
                    cn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            throw new RuntimeException("Error al registrar pago", e);
        } finally {
            if (cn != null) {
                try {
                    cn.setAutoCommit(true);
                    cn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public int countPendingInstallments(long pagoId) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM pago_plazo
                WHERE pago_id = ? AND estado = ?
                """;

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, pagoId);
            ps.setString(2, PaymentInstallmentStatus.PENDIENTE.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar plazos pendientes", e);
        }
    }

    @Override
    public void updatePaymentStatus(long pagoId, PaymentStatus status) {
        String sql = "UPDATE pago SET estado = ? WHERE id = ?";

        try (Connection cn = dataSource.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, mapStatusToDb(status));
            ps.setLong(2, pagoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar estado del pago", e);
        }
    }

    private void insertInstallments(long paymentId, List<PaymentInstallment> installments, Connection connection) throws SQLException {
        String sql = """
                INSERT INTO pago_plazo
                    (pago_id, numero, fecha_vencimiento, importe, estado, fecha_pago)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (PaymentInstallment installment : installments) {
                int i = 1;
                ps.setLong(i++, paymentId);
                ps.setInt(i++, installment.getNumero() != null ? installment.getNumero() : 1);
                if (installment.getFechaVencimiento() != null) {
                    ps.setDate(i++, java.sql.Date.valueOf(installment.getFechaVencimiento()));
                } else {
                    ps.setNull(i++, Types.DATE);
                }
                ps.setBigDecimal(i++, installment.getImporte() != null ? installment.getImporte() : BigDecimal.ZERO);
                ps.setString(i++, mapInstallmentStatusToDb(installment.getEstado()));
                if (installment.getFechaPago() != null) {
                    ps.setTimestamp(i++, Timestamp.valueOf(installment.getFechaPago()));
                } else {
                    ps.setNull(i++, Types.TIMESTAMP);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertRecords(long paymentId, List<PaymentRecord> records, Connection connection) throws SQLException {
        String sql = """
                INSERT INTO pago_registro
                    (pago_id, plazo_id, fecha, importe, observaciones)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (PaymentRecord record : records) {
                int i = 1;
                ps.setLong(i++, paymentId);
                if (record.getPlazoId() != null) {
                    ps.setLong(i++, record.getPlazoId());
                } else {
                    ps.setNull(i++, Types.BIGINT);
                }
                LocalDateTime fecha = record.getFecha() != null ? record.getFecha() : LocalDateTime.now();
                ps.setTimestamp(i++, Timestamp.valueOf(fecha));
                ps.setBigDecimal(i++, record.getImporte() != null ? record.getImporte() : BigDecimal.ZERO);
                ps.setString(i++, record.getObservaciones());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void updateTotalPagado(long pagoId, Connection connection) throws SQLException {
        String sql = """
                UPDATE pago
                SET total_pagado = (
                    SELECT COALESCE(SUM(importe), 0)
                    FROM pago_registro
                    WHERE pago_id = ?
                )
                WHERE id = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, pagoId);
            ps.setLong(2, pagoId);
            ps.executeUpdate();
        }
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setId(rs.getLong("id"));
        payment.setEmpresaId(rs.getLong("empresa_id"));
        payment.setFacturaId(rs.getLong("factura_id"));
        payment.setClienteId(rs.getLong("cliente_id"));
        payment.setTipo(mapTypeFromDb(rs.getString("tipo")));
        payment.setEstado(mapStatusFromDb(rs.getString("estado")));
        payment.setTotal(rs.getBigDecimal("total"));
        payment.setTotalPagado(rs.getBigDecimal("total_pagado"));
        int numeroPlazos = rs.getInt("numero_plazos");
        if (!rs.wasNull()) {
            payment.setNumeroPlazos(numeroPlazos);
        }
        payment.setInteresPorcentaje(rs.getBigDecimal("interes_porcentaje"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            payment.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            payment.setUpdatedAt(updated.toLocalDateTime());
        }
        payment.setEmpresaNombre(rs.getString("empresa_nombre"));
        payment.setClienteNombre(rs.getString("cliente_nombre"));
        payment.setFacturaNumero(rs.getString("factura_numero"));
        return payment;
    }

    private PaymentInstallment mapInstallment(ResultSet rs) throws SQLException {
        PaymentInstallment installment = new PaymentInstallment();
        installment.setId(rs.getLong("id"));
        installment.setPagoId(rs.getLong("pago_id"));
        installment.setNumero(rs.getInt("numero"));
        java.sql.Date fechaVenc = rs.getDate("fecha_vencimiento");
        if (fechaVenc != null) {
            installment.setFechaVencimiento(fechaVenc.toLocalDate());
        }
        installment.setImporte(rs.getBigDecimal("importe"));
        installment.setEstado(mapInstallmentStatusFromDb(rs.getString("estado")));
        Timestamp fechaPago = rs.getTimestamp("fecha_pago");
        if (fechaPago != null) {
            installment.setFechaPago(fechaPago.toLocalDateTime());
        }
        return installment;
    }

    private PaymentRecord mapRecord(ResultSet rs) throws SQLException {
        PaymentRecord record = new PaymentRecord();
        record.setId(rs.getLong("id"));
        record.setPagoId(rs.getLong("pago_id"));
        long plazoId = rs.getLong("plazo_id");
        if (!rs.wasNull()) {
            record.setPlazoId(plazoId);
        }
        Timestamp fecha = rs.getTimestamp("fecha");
        if (fecha != null) {
            record.setFecha(fecha.toLocalDateTime());
        }
        record.setImporte(rs.getBigDecimal("importe"));
        record.setObservaciones(rs.getString("observaciones"));
        return record;
    }

    private String baseSelect() {
        return """
                SELECT p.id,
                    p.empresa_id,
                    p.factura_id,
                    p.cliente_id,
                    p.tipo,
                    p.estado,
                    p.total,
                    p.total_pagado,
                    p.numero_plazos,
                    p.interes_porcentaje,
                    p.created_at,
                    p.updated_at,
                    e.nombre AS empresa_nombre,
                    c.nombre AS cliente_nombre,
                    f.numero AS factura_numero
                FROM pago p
                JOIN empresa e ON e.id = p.empresa_id
                JOIN cliente c ON c.id = p.cliente_id
                JOIN factura f ON f.id = p.factura_id
                """;
    }

    private String mapStatusToDb(PaymentStatus status) {
        if (status == null) {
            return PaymentStatus.ABIERTO.name();
        }
        return status.name();
    }

    private PaymentStatus mapStatusFromDb(String value) {
        if (value == null) {
            return PaymentStatus.ABIERTO;
        }
        return PaymentStatus.valueOf(value);
    }

    private String mapTypeToDb(PaymentType type) {
        if (type == null) {
            return PaymentType.CONTADO.name();
        }
        return type.name();
    }

    private PaymentType mapTypeFromDb(String value) {
        if (value == null) {
            return PaymentType.CONTADO;
        }
        return PaymentType.valueOf(value);
    }

    private String mapInstallmentStatusToDb(PaymentInstallmentStatus status) {
        if (status == null) {
            return PaymentInstallmentStatus.PENDIENTE.name();
        }
        return status.name();
    }

    private PaymentInstallmentStatus mapInstallmentStatusFromDb(String value) {
        if (value == null) {
            return PaymentInstallmentStatus.PENDIENTE;
        }
        return PaymentInstallmentStatus.valueOf(value);
    }
}
