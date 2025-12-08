package com.gearmind.application.customer;

import com.gearmind.domain.customer.Customer;
import com.gearmind.domain.customer.CustomerRepository;
import java.util.regex.Pattern;

public class SaveCustomerUseCase {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final CustomerRepository repository;

    public SaveCustomerUseCase(CustomerRepository repository) {
        this.repository = repository;
    }

    public Customer save(SaveCustomerRequest request) {
        validate(request);

        if (request.id() == null) {
            return repository.create(request.empresaId(), request.nombre().trim(), normalize(request.email()), normalize(request.telefono()), normalize(request.notas()));
        }

        return repository.update(request.id(), request.empresaId(), request.nombre().trim(), normalize(request.email()), normalize(request.telefono()), normalize(request.notas()));
    }

    private String normalize(String s) {
        return s == null ? null : s.trim();
    }

    private void validate(SaveCustomerRequest r) {
        if (r.empresaId() <= 0) {
            throw new IllegalArgumentException("empresaId debe ser > 0");
        }

        if (r.nombre() == null || r.nombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }

        if (r.nombre().trim().length() > 150) {
            throw new IllegalArgumentException("El nombre no puede superar 150 caracteres.");
        }

        if (r.email() != null && !r.email().trim().isEmpty()) {
            String email = r.email().trim();
            if (email.length() > 150) {
                throw new IllegalArgumentException("El email no puede superar 150 caracteres.");
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("El email no tiene un formato válido.");
            }
        }

        if (r.telefono() != null && !r.telefono().trim().isEmpty()) {
            String tel = r.telefono().trim();
            if (tel.length() > 20) {
                throw new IllegalArgumentException("El teléfono no puede superar 20 caracteres.");
            }
            if (!tel.matches("[0-9+ ]+")) {
                throw new IllegalArgumentException("El teléfono sólo puede contener dígitos, espacios y '+'.");
            }
        }
    }
}
