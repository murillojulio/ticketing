package com.ticketing.platform.infrastructure.persistence.inmemory;

import com.ticketing.platform.application.port.out.RoleRepository;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.model.Role;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@ConditionalOnProperty(prefix = "ticketing.adapters", name = "persistence", havingValue = "inmemory")
public class InMemoryRoleRepository implements RoleRepository {

    private final ConcurrentMap<UUID, Role> rolesById = new ConcurrentHashMap<>();

    @Override
    public Mono<Role> create(Role role) {
        Role previous = rolesById.putIfAbsent(role.id(), role);
        if (previous != null) {
            return Mono.error(new DomainException("Role already exists"));
        }
        return Mono.just(role);
    }

    @Override
    public Mono<Role> update(Role role) {
        if (!rolesById.containsKey(role.id())) {
            return Mono.error(new DomainException("Role not found"));
        }
        rolesById.put(role.id(), role);
        return Mono.just(role);
    }

    @Override
    public Mono<Role> findById(UUID id) {
        return Mono.justOrEmpty(rolesById.get(id));
    }

    @Override
    public Mono<Role> findByName(String name) {
        return Flux.fromIterable(rolesById.values())
                .filter(r -> r.name().equals(name))
                .next();
    }

    @Override
    public Flux<Role> findAll() {
        return Flux.fromIterable(rolesById.values());
    }
}
