package com.gearmind.infrastructure.email;

import com.gearmind.common.exception.InfrastructureException;
import com.gearmind.common.exception.ValidationException;
import com.gearmind.domain.email.EmailConfig;
import com.gearmind.domain.email.SmtpConfigRepository;
import com.gearmind.infrastructure.database.DataSourceFactory;
import com.gearmind.infrastructure.security.AesCipher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

public class JdbcSmtpConfigRepository implements SmtpConfigRepository {

    private final DataSource dataSource;
    private final AesCipher cipher;

    public JdbcSmtpConfigRepository() {
        this(DataSourceFactory.getDataSource(), new AesCipher());
    }

    public JdbcSmtpConfigRepository(DataSource dataSource, AesCipher cipher) {
        this.dataSource = dataSource;
        this.cipher = cipher;
    }

    @Override
    public EmailConfig findByEmpresaId(long empresaId) {
        String sql = """
            SELECT host, port, username, password_encrypted, from_address, starttls, `ssl`
            FROM smtp_config
            WHERE empresa_id = ?
            """;
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String password = cipher.decrypt(rs.getString("password_encrypted"));
                    return new EmailConfig(rs.getString("host"), rs.getInt("port"), rs.getString("username"), password, rs.getString("from_address"), rs.getBoolean("starttls"),rs.getBoolean("ssl"));
                }
            }
            throw new ValidationException("No hay configuración SMTP para la empresa.");
        } catch (SQLException e) {
            throw new InfrastructureException("Error cargando la configuración SMTP.", e);
        }
    }

    @Override
    public void save(long empresaId, EmailConfig config) {
        String sql = """
            INSERT INTO smtp_config (empresa_id, host, port, username, password_encrypted, from_address, starttls, `ssl`, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        executeUpsert(sql, empresaId, config);
    }

    @Override
    public void update(long empresaId, EmailConfig config) {
        String sql = """
            UPDATE smtp_config
            SET host = ?, port = ?, username = ?, password_encrypted = ?, from_address = ?, starttls = ?, `ssl` = ?, updated_at = NOW()
            WHERE empresa_id = ?
            """;
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindConfig(ps, config);
            ps.setLong(8, empresaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new InfrastructureException("Error actualizando la configuración SMTP.", e);
        }
    }

    private void executeUpsert(String sql, long empresaId, EmailConfig config) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, empresaId);
            bindConfig(ps, config, 2);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new InfrastructureException("Error guardando la configuración SMTP.", e);
        }
    }

    private void bindConfig(PreparedStatement ps, EmailConfig config) throws SQLException {
        bindConfig(ps, config, 1);
    }

    private void bindConfig(PreparedStatement ps, EmailConfig config, int startIndex) throws SQLException {
        ps.setString(startIndex, config.getHost());
        ps.setInt(startIndex + 1, config.getPort());
        ps.setString(startIndex + 2, config.getUsername());
        ps.setString(startIndex + 3, cipher.encrypt(config.getPassword()));
        ps.setString(startIndex + 4, config.getFrom());
        ps.setBoolean(startIndex + 5, config.isStartTls());
        ps.setBoolean(startIndex + 6, config.isSsl());
    }
}
