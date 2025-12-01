package com.gearmind.application.auth;

import com.gearmind.domain.security.PasswordHasher;
import com.gearmind.domain.user.User;
import com.gearmind.domain.user.UserRepository;

public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public LoginUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
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

        return new LoginResponse(user);
    }
}
