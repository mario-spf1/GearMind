package com.gearmind.domain.appointment;

public class OverlappingAppointmentException extends RuntimeException {

    public OverlappingAppointmentException(String message) {
        super(message);
    }
}
