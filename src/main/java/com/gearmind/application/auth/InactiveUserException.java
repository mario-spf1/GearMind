package com.gearmind.application.auth;

public class InactiveUserException extends RuntimeException {
    public InactiveUserException() {
        super("Usuario inactivo");
    }
}
