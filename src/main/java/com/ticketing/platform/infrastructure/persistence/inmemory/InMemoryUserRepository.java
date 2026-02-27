package com.ticketing.platform.infrastructure.persistence.inmemory;

import com.ticketing.platform.application.port.out.UserRepository;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.model.AppUser;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@ConditionalOnProperty(prefix = "ticketing.adapters", name = "persistence", havingValue = "inmemory")
public class InMemoryUserRepository implements UserRepository {

    private final ConcurrentMap<String, AppUser> usersByEmail = new ConcurrentHashMap<>();

    @Override
    public Mono<AppUser> create(AppUser user) {
        AppUser previous = usersByEmail.putIfAbsent(AppUser.normalizeEmail(user.email()), user);
        if (previous != null) {
            return Mono.error(new DomainException("User already exists"));
        }
        return Mono.just(user);
    }

    @Override
    public Mono<AppUser> findByEmail(String email) {
        return Mono.justOrEmpty(usersByEmail.get(AppUser.normalizeEmail(email)));
    }

    @Override
    public Mono<AppUser> findById(UUID id) {
        return Mono.justOrEmpty(
                usersByEmail.values().stream()
                        .filter(u -> u.id().equals(id))
                        .findFirst());
    }

    @Override
    public Mono<AppUser> update(AppUser user) {
        if (!usersByEmail.containsKey(AppUser.normalizeEmail(user.email()))) {
            return Mono.error(new DomainException("User not found"));
        }
        usersByEmail.put(AppUser.normalizeEmail(user.email()), user);
        return Mono.just(user);
    }
}
