package com.gearmind.application.auth;

import com.gearmind.domain.company.Empresa;
import com.gearmind.domain.company.EmpresaRepository;
import com.gearmind.domain.security.PasswordHasher;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRepository;
import java.util.Optional;

public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final EmpresaRepository empresaRepository;

    public LoginUseCase(UserRepository userRepository, PasswordHasher passwordHasher, EmpresaRepository empresaRepository) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.empresaRepository = empresaRepository;
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        String password = request.password();

        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);

        if (!user.isActivo()) {
            throw new InactiveUserException();
        }

        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Long empresaId = user.getEmpresaId();
        String empresaNombre = null;

        if (empresaId != null) {
            Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);
            empresaNombre = empresaOpt.map(Empresa::getNombre).orElse(null);
        }

        return new LoginResponse(user, empresaId, empresaNombre);
    }
}
