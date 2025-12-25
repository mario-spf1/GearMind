package com.gearmind.domain.task;

import java.time.LocalDateTime;

public class Task {

    private Long id;
    private Long empresaId;
    private Long ordenTrabajoId;
    private Long asignadoA;
    private String titulo;
    private String descripcion;
    private TaskStatus estado;
    private TaskPriority prioridad;
    private LocalDateTime fechaLimite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String empresaNombre;
    private String empleadoNombre;
    private String repairDescripcion;
    private String clienteNombre;
    private String vehiculoEtiqueta;
    private String vehiculoMatricula;

    public Task() {
    }

    public Task(Long id, Long empresaId, Long ordenTrabajoId, Long asignadoA, String titulo, String descripcion, TaskStatus estado, TaskPriority prioridad, LocalDateTime fechaLimite, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.empresaId = empresaId;
        this.ordenTrabajoId = ordenTrabajoId;
        this.asignadoA = asignadoA;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.estado = estado;
        this.prioridad = prioridad;
        this.fechaLimite = fechaLimite;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Long getOrdenTrabajoId() {
        return ordenTrabajoId;
    }

    public void setOrdenTrabajoId(Long ordenTrabajoId) {
        this.ordenTrabajoId = ordenTrabajoId;
    }

    public Long getAsignadoA() {
        return asignadoA;
    }

    public void setAsignadoA(Long asignadoA) {
        this.asignadoA = asignadoA;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TaskStatus getEstado() {
        return estado;
    }

    public void setEstado(TaskStatus estado) {
        this.estado = estado;
    }

    public TaskPriority getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(TaskPriority prioridad) {
        this.prioridad = prioridad;
    }

    public LocalDateTime getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDateTime fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEmpresaNombre() {
        return empresaNombre;
    }

    public void setEmpresaNombre(String empresaNombre) {
        this.empresaNombre = empresaNombre;
    }

    public String getEmpleadoNombre() {
        return empleadoNombre;
    }

    public void setEmpleadoNombre(String empleadoNombre) {
        this.empleadoNombre = empleadoNombre;
    }

    public String getRepairDescripcion() {
        return repairDescripcion;
    }

    public void setRepairDescripcion(String repairDescripcion) {
        this.repairDescripcion = repairDescripcion;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getVehiculoEtiqueta() {
        return vehiculoEtiqueta;
    }

    public void setVehiculoEtiqueta(String vehiculoEtiqueta) {
        this.vehiculoEtiqueta = vehiculoEtiqueta;
    }

    public String getVehiculoMatricula() {
        return vehiculoMatricula;
    }

    public void setVehiculoMatricula(String vehiculoMatricula) {
        this.vehiculoMatricula = vehiculoMatricula;
    }
}
