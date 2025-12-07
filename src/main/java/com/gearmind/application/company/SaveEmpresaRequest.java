package com.gearmind.application.company;

public record SaveEmpresaRequest(Long id, String nombre, String cif, String telefono, String email, String direccion, String ciudad, String provincia, String cp, Boolean activa) {}
