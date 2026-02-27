package com.ticketing.platform.application.port.out;

import com.ticketing.platform.domain.model.Role;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleRepository {
    Mono<Role> create(Role role);

    Mono<Role> update(Role role);

    Mono<Role> findById(UUID id);

    Mono<Role> findByName(String name);

    Flux<Role> findAll();
}
