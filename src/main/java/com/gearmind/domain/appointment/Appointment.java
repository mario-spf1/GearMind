package com.gearmind.domain.appointment;

import java.time.LocalDateTime;

public class Appointment {

    private Long id;
    private Long empresaId;
    private Long employeeId;
    private Long customerId;
    private Long vehicleId;
    private LocalDateTime dateTime;
    private AppointmentStatus status;
    private AppointmentOrigin origin;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Appointment() {
    }

    public Appointment(Long id, Long empresaId, Long employeeId, Long customerId, Long vehicleId, LocalDateTime dateTime, AppointmentStatus status, AppointmentOrigin origin, String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.empresaId = empresaId;
        this.employeeId = employeeId;
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.dateTime = dateTime;
        this.status = status;
        this.origin = origin;
        this.notes = notes;
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

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public AppointmentOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(AppointmentOrigin origin) {
        this.origin = origin;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
}
