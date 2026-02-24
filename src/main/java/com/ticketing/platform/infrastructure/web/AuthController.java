package com.ticketing.platform.infrastructure.web;

import com.ticketing.platform.application.model.LoginUserCommand;
import com.ticketing.platform.application.model.RegisterUserCommand;
import com.ticketing.platform.application.port.in.AuthUseCase;
import com.ticketing.platform.infrastructure.web.dto.AuthResponse;
import com.ticketing.platform.infrastructure.web.dto.LoginRequest;
import com.ticketing.platform.infrastructure.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authUseCase.register(
            new RegisterUserCommand(
                request.email(),
                request.password()
            )
        ).map(AuthResponse::from);
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return authUseCase.login(
            new LoginUserCommand(
                request.email(),
                request.password()
            )
        ).map(AuthResponse::from);
    }
}
