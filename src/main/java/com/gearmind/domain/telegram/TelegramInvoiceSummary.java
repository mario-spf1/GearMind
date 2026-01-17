package com.gearmind.domain.telegram;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TelegramInvoiceSummary {

    private final long id;
    private final String numero;
    private final LocalDate fecha;
    private final String estado;
    private final BigDecimal total;

    public TelegramInvoiceSummary(long id, String numero, LocalDate fecha, String estado, BigDecimal total) {
        this.id = id;
        this.numero = numero;
        this.fecha = fecha;
        this.estado = estado;
        this.total = total;
    }

    public long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getEstado() {
        return estado;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
