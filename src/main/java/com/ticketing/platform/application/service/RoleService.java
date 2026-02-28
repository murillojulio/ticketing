package com.ticketing.platform.application.service;

import com.ticketing.platform.application.port.in.RoleUseCase;
import com.ticketing.platform.application.port.out.RoleRepository;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.model.Role;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RoleService implements RoleUseCase {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Mono<Role> createRole(String name, Set<String> permissions) {
        return Mono.defer(() -> {
            if (name == null || name.isBlank()) {
                return Mono.error(new DomainException("Role name is required"));
            }
            Role role = new Role(UUID.randomUUID(), name, permissions);
            return roleRepository.findByName(name)
                    .flatMap(existing -> Mono.<Role>error(new DomainException("Role already exists")))
                    .switchIfEmpty(roleRepository.create(role));
        });
    }

    @Override
    public Mono<Role> updateRolePermissions(UUID id, Set<String> permissions) {
        return roleRepository.findById(id)
                .switchIfEmpty(Mono.error(new DomainException("Role not found")))
                .flatMap(role -> {
                    Role updated = new Role(role.id(), role.name(), permissions);
                    return roleRepository.update(updated);
                });
    }

    @Override
    public Mono<Role> getRole(UUID id) {
        return roleRepository.findById(id)
                .switchIfEmpty(Mono.error(new DomainException("Role not found")));
    }

    @Override
    public Flux<Role> listRoles() {
        return roleRepository.findAll();
    }
}
