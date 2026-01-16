package com.gearmind.infrastructure.message;

import com.gearmind.domain.message.ConversationSummary;
import com.gearmind.domain.message.InternalMessage;
import com.gearmind.domain.message.MessageRepository;
import com.gearmind.infrastructure.database.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MySqlMessageRepository implements MessageRepository {

    private final DataSource dataSource;

    public MySqlMessageRepository() {
        this(DataSourceFactory.getDataSource());
    }

    public MySqlMessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<ConversationSummary> findConversations(long empresaId, long userId) {
        String sql = """
                SELECT u.id AS contacto_id,
                       u.nombre AS contacto_nombre,
                       m.contenido AS ultimo_mensaje,
                       m.created_at AS ultimo_mensaje_at
                FROM (
                    SELECT CASE
                               WHEN sender_id = ? THEN receiver_id
                               ELSE sender_id
                           END AS other_id,
                           MAX(created_at) AS last_time
                    FROM mensaje_interno
                    WHERE empresa_id = ? AND (sender_id = ? OR receiver_id = ?)
                    GROUP BY other_id
                ) t
                JOIN mensaje_interno m
                  ON ((m.sender_id = ? AND m.receiver_id = t.other_id)
                   OR (m.sender_id = t.other_id AND m.receiver_id = ?))
                 AND m.created_at = t.last_time
                JOIN usuario u ON u.id = t.other_id
                ORDER BY t.last_time DESC
                """;
        List<ConversationSummary> result = new ArrayList<>();

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, userId);
            ps.setLong(i++, empresaId);
            ps.setLong(i++, userId);
            ps.setLong(i++, userId);
            ps.setLong(i++, userId);
            ps.setLong(i++, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapConversation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando conversaciones internas", e);
        }

        return result;
    }

    @Override
    public List<InternalMessage> findMessages(long empresaId, long userId, long otherUserId) {
        String sql = """
                SELECT id, empresa_id, sender_id, receiver_id, contenido, created_at, read_at
                FROM mensaje_interno
                WHERE empresa_id = ?
                  AND ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?))
                ORDER BY created_at ASC
                """;
        List<InternalMessage> result = new ArrayList<>();

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, empresaId);
            ps.setLong(i++, userId);
            ps.setLong(i++, otherUserId);
            ps.setLong(i++, otherUserId);
            ps.setLong(i++, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapMessage(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando mensajes internos", e);
        }

        return result;
    }

    @Override
    public InternalMessage sendMessage(long empresaId, long senderId, long receiverId, String contenido) {
        String sql = """
                INSERT INTO mensaje_interno (empresa_id, sender_id, receiver_id, contenido, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setLong(i++, empresaId);
            ps.setLong(i++, senderId);
            ps.setLong(i++, receiverId);
            ps.setString(i++, contenido);
            ps.setTimestamp(i, Timestamp.valueOf(now));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new InternalMessage(id, empresaId, senderId, receiverId, contenido, now, null);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error enviando mensaje interno", e);
        }

        return new InternalMessage(null, empresaId, senderId, receiverId, contenido, now, null);
    }

    private ConversationSummary mapConversation(ResultSet rs) throws SQLException {
        long contactoId = rs.getLong("contacto_id");
        String contactoNombre = rs.getString("contacto_nombre");
        String ultimoMensaje = rs.getString("ultimo_mensaje");
        Timestamp ts = rs.getTimestamp("ultimo_mensaje_at");
        LocalDateTime lastAt = ts != null ? ts.toLocalDateTime() : null;

        return new ConversationSummary(contactoId, contactoNombre, ultimoMensaje, lastAt);
    }

    private InternalMessage mapMessage(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long empresaId = rs.getLong("empresa_id");
        long senderId = rs.getLong("sender_id");
        long receiverId = rs.getLong("receiver_id");
        String contenido = rs.getString("contenido");
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp read = rs.getTimestamp("read_at");

        return new InternalMessage(id, empresaId, senderId, receiverId, contenido, created != null ? created.toLocalDateTime() : null, read != null ? read.toLocalDateTime() : null);
    }
}
