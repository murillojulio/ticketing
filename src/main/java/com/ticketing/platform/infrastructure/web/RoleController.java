package com.ticketing.platform.infrastructure.web;

import com.ticketing.platform.application.port.in.RoleUseCase;
import com.ticketing.platform.domain.model.Role;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleUseCase roleUseCase;

    public RoleController(RoleUseCase roleUseCase) {
        this.roleUseCase = roleUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:write')")
    public Mono<Role> createRole(@RequestBody CreateRoleRequest request) {
        return roleUseCase.createRole(request.name(), request.permissions());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:write')")
    public Mono<Role> updateRolePermissions(@PathVariable UUID id, @RequestBody UpdateRoleRequest request) {
        return roleUseCase.updateRolePermissions(id, request.permissions());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public Flux<Role> listRoles() {
        return roleUseCase.listRoles();
    }

    public record CreateRoleRequest(String name, Set<String> permissions) {
    }

    public record UpdateRoleRequest(Set<String> permissions) {
    }
}
