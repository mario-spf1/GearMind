package com.gearmind.domain.email;

public interface SmtpConfigRepository {

    EmailConfig findByEmpresaId(long empresaId);

    void save(long empresaId, EmailConfig config);

    void update(long empresaId, EmailConfig config);
}
