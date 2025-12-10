package com.gearmind.application.appointment;

import com.gearmind.domain.appointment.AppointmentOrigin;

import java.time.LocalDateTime;

public class SaveAppointmentRequest {

    private Long id;
    private Long empresaId;
    private Long customerId;
    private Long vehicleId;
    private LocalDateTime dateTime;
    private String notes;
    private AppointmentOrigin origin;

    public SaveAppointmentRequest() {
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public AppointmentOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(AppointmentOrigin origin) {
        this.origin = origin;
    }
}
