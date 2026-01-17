package com.gearmind.application.customer;

public record SaveCustomerRequest(Long id, long empresaId, String nombre, String dni, String email, String telefono, String notas) {

}
