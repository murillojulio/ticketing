package com.ticketing.platform.infrastructure.web;

import com.ticketing.platform.application.port.in.UserUseCase;
import com.ticketing.platform.domain.model.AppUser;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public Flux<UserResponse> listUsers() {
        return userUseCase.listUsers()
                .map(this::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public Mono<UserResponse> getUser(@PathVariable UUID id) {
        return userUseCase.getUser(id)
                .map(this::toResponse);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    public Mono<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return userUseCase.createUser(new UserUseCase.CreateUserCommand(
                request.email(),
                request.password(),
                request.roles()))
                .map(this::toResponse);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    public Mono<UserResponse> updateUser(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        return userUseCase.updateUser(new UserUseCase.UpdateUserCommand(
                id,
                request.email(),
                request.roles()))
                .map(this::toResponse);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    public Mono<Void> deleteUser(@PathVariable UUID id) {
        return userUseCase.deleteUser(id);
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(user.id(), user.email(), user.roles());
    }

    public record UserResponse(UUID id, String email, Set<String> roles) {
    }

    public record CreateUserRequest(String email, String password, Set<String> roles) {
    }

    public record UpdateUserRequest(String email, Set<String> roles) {
    }
}
