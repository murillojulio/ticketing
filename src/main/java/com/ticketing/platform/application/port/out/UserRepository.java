package com.ticketing.platform.application.port.out;

import com.ticketing.platform.domain.model.AppUser;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface UserRepository {

    Mono<AppUser> create(AppUser user);

    Mono<AppUser> update(AppUser user);

    Mono<AppUser> findByEmail(String email);

    Mono<AppUser> findById(UUID id);
}
