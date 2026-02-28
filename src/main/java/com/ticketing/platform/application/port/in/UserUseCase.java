package com.ticketing.platform.application.port.in;

import com.ticketing.platform.domain.model.AppUser;
import java.util.Set;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserUseCase {

    Flux<AppUser> listUsers();

    Mono<AppUser> getUser(UUID id);

    Mono<AppUser> createUser(CreateUserCommand command);

    Mono<AppUser> updateUser(UpdateUserCommand command);

    Mono<Void> deleteUser(UUID id);

    public record CreateUserCommand(String email, String password, Set<String> roles) {
    }

    public record UpdateUserCommand(UUID id, String email, Set<String> roles) {
    }
}
