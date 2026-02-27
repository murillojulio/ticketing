package com.ticketing.platform.infrastructure.web;

import com.ticketing.platform.application.port.in.UserRoleUseCase;
import com.ticketing.platform.domain.model.AppUser;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users/{userId}/roles")
public class UserRoleController {

    private final UserRoleUseCase userRoleUseCase;

    public UserRoleController(UserRoleUseCase userRoleUseCase) {
        this.userRoleUseCase = userRoleUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user_role:write')")
    public Mono<UserRoleResponse> assignRoles(@PathVariable UUID userId, @RequestBody RolesRequest request) {
        return userRoleUseCase.assignRoles(userId, request.roles())
                .map(this::toResponse);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('user_role:write')")
    public Mono<UserRoleResponse> removeRoles(@PathVariable UUID userId, @RequestBody RolesRequest request) {
        return userRoleUseCase.removeRoles(userId, request.roles())
                .map(this::toResponse);
    }

    private UserRoleResponse toResponse(AppUser user) {
        return new UserRoleResponse(user.id(), user.email(), user.roles());
    }

    public record RolesRequest(Set<String> roles) {
    }

    public record UserRoleResponse(UUID id, String email, Set<String> roles) {
    }
}
