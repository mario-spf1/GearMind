package com.gearmind.application.task;

import com.gearmind.domain.task.TaskPriority;
import com.gearmind.domain.task.TaskStatus;

import java.time.LocalDateTime;

public class SaveTaskRequest {

    private Long id;
    private Long empresaId;
    private Long ordenTrabajoId;
    private Long asignadoA;
    private String titulo;
    private String descripcion;
    private TaskStatus estado;
    private TaskPriority prioridad;
    private LocalDateTime fechaLimite;

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
}
