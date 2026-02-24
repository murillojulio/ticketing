package com.ticketing.platform.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.model.LoginUserCommand;
import com.ticketing.platform.application.model.RegisterUserCommand;
import com.ticketing.platform.application.port.out.AuthTokenPort;
import com.ticketing.platform.application.port.out.PasswordHasherPort;
import com.ticketing.platform.application.port.out.UserRepository;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.model.AppUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasherPort passwordHasherPort;

    @Mock
    private AuthTokenPort authTokenPort;

    @Test
    void shouldRegisterUserAndReturnToken() {
        AuthService authService = new AuthService(userRepository, passwordHasherPort, authTokenPort);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Mono.empty());
        when(passwordHasherPort.hash("password123")).thenReturn("hashed-password");
        when(userRepository.create(any(AppUser.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(authTokenPort.generate(any(AppUser.class))).thenReturn(Mono.just("jwt-token"));

        var result = authService.register(new RegisterUserCommand("User@Example.com", "password123")).block();

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void shouldRejectDuplicatedUserRegistration() {
        AuthService authService = new AuthService(userRepository, passwordHasherPort, authTokenPort);
        AppUser existing = AppUser.create(UUID.randomUUID(), "user@example.com", "hash");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Mono.just(existing));

        assertThatThrownBy(() -> authService.register(
            new RegisterUserCommand("user@example.com", "password123")
        ).block()).isInstanceOf(DomainException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldLoginWithValidCredentials() {
        AuthService authService = new AuthService(userRepository, passwordHasherPort, authTokenPort);
        AppUser user = AppUser.create(UUID.randomUUID(), "user@example.com", "hash");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Mono.just(user));
        when(passwordHasherPort.matches("password123", "hash")).thenReturn(true);
        when(authTokenPort.generate(user)).thenReturn(Mono.just("jwt-token"));

        var result = authService.login(new LoginUserCommand("user@example.com", "password123")).block();

        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt-token");
    }

    @Test
    void shouldRejectInvalidCredentialsOnLogin() {
        AuthService authService = new AuthService(userRepository, passwordHasherPort, authTokenPort);
        AppUser user = AppUser.create(UUID.randomUUID(), "user@example.com", "hash");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Mono.just(user));
        when(passwordHasherPort.matches("wrong-password", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
            new LoginUserCommand("user@example.com", "wrong-password")
        ).block()).isInstanceOf(DomainException.class)
            .hasMessageContaining("Invalid credentials");
    }
}
