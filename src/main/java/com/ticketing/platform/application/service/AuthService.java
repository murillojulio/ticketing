package com.ticketing.platform.application.service;

import com.ticketing.platform.application.model.AuthResult;
import com.ticketing.platform.application.model.LoginUserCommand;
import com.ticketing.platform.application.model.RegisterUserCommand;
import com.ticketing.platform.application.port.in.AuthUseCase;
import com.ticketing.platform.application.port.out.AuthTokenPort;
import com.ticketing.platform.application.port.out.PasswordHasherPort;
import com.ticketing.platform.application.port.out.UserRepository;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.model.AppUser;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasherPort;
    private final AuthTokenPort authTokenPort;

    public AuthService(
        UserRepository userRepository,
        PasswordHasherPort passwordHasherPort,
        AuthTokenPort authTokenPort
    ) {
        this.userRepository = userRepository;
        this.passwordHasherPort = passwordHasherPort;
        this.authTokenPort = authTokenPort;
    }

    @Override
    public Mono<AuthResult> register(RegisterUserCommand command) {
        return Mono.defer(() -> {
            validateCredentials(command.email(), command.password());
            String normalizedEmail = AppUser.normalizeEmail(command.email());
            return userRepository.findByEmail(normalizedEmail)
                .flatMap(existingUser -> Mono.<AppUser>error(new DomainException("User already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    AppUser newUser = AppUser.create(
                        UUID.randomUUID(),
                        normalizedEmail,
                        passwordHasherPort.hash(command.password())
                    );
                    return userRepository.create(newUser);
                }))
                .flatMap(this::toAuthResult);
        });
    }

    @Override
    public Mono<AuthResult> login(LoginUserCommand command) {
        return Mono.defer(() -> {
            validateCredentials(command.email(), command.password());
            String normalizedEmail = AppUser.normalizeEmail(command.email());
            return userRepository.findByEmail(normalizedEmail)
                .switchIfEmpty(Mono.error(new DomainException("Invalid credentials")))
                .flatMap(user -> {
                    boolean matches = passwordHasherPort.matches(command.password(), user.passwordHash());
                    if (!matches) {
                        return Mono.error(new DomainException("Invalid credentials"));
                    }
                    return toAuthResult(user);
                });
        });
    }

    private Mono<AuthResult> toAuthResult(AppUser user) {
        return authTokenPort.generate(user)
            .map(token -> new AuthResult(
                user.id(),
                user.email(),
                user.role(),
                token,
                "Bearer"
            ));
    }

    private static void validateCredentials(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new DomainException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new DomainException("Password is required");
        }
    }
}
