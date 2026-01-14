package com.gearmind.domain.fichaje;

import java.time.LocalDateTime;

public class Fichaje {

    private Long id;
    private Long empresaId;
    private Long userId;
    private FichajeMovimiento movimiento;
    private LocalDateTime fecha;
    private String usuarioNombre;
    private String empresaNombre;

    public Fichaje() {
    }

    public Fichaje(Long id, Long empresaId, Long userId, FichajeMovimiento movimiento, LocalDateTime fecha) {
        this.id = id;
        this.empresaId = empresaId;
        this.userId = userId;
        this.movimiento = movimiento;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public FichajeMovimiento getMovimiento() {
        return movimiento;
    }

    public void setMovimiento(FichajeMovimiento movimiento) {
        this.movimiento = movimiento;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getEmpresaNombre() {
        return empresaNombre;
    }

    public void setEmpresaNombre(String empresaNombre) {
        this.empresaNombre = empresaNombre;
    }
}
