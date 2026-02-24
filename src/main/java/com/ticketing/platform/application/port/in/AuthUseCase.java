package com.ticketing.platform.application.port.in;

import com.ticketing.platform.application.model.AuthResult;
import com.ticketing.platform.application.model.LoginUserCommand;
import com.ticketing.platform.application.model.RegisterUserCommand;
import reactor.core.publisher.Mono;

public interface AuthUseCase {

    Mono<AuthResult> register(RegisterUserCommand command);

    Mono<AuthResult> login(LoginUserCommand command);
}
