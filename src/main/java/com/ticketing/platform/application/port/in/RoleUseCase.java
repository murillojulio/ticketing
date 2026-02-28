package com.ticketing.platform.application.port.in;

import com.ticketing.platform.domain.model.Role;
import java.util.Set;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoleUseCase {
    Mono<Role> createRole(String name, Set<String> permissions);

    Mono<Role> updateRolePermissions(UUID id, Set<String> permissions);

    Mono<Role> getRole(UUID id);

    Flux<Role> listRoles();
}
