package com.ticketing.platform.application.port.in;

import com.ticketing.platform.domain.model.AppUser;
import java.util.Set;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface UserRoleUseCase {
    Mono<AppUser> assignRoles(UUID userId, Set<String> roles);

    Mono<AppUser> removeRoles(UUID userId, Set<String> roles);
}
