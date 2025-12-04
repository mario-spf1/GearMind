package com.gearmind.application.customer;

public record SaveCustomerRequest(Long id, long empresaId, String nombre, String email, String telefono, String notas) {}
