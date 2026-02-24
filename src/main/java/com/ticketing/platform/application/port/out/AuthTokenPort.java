package com.ticketing.platform.application.port.out;

import com.ticketing.platform.domain.model.AppUser;
import reactor.core.publisher.Mono;

public interface AuthTokenPort {

    Mono<String> generate(AppUser user);
}
