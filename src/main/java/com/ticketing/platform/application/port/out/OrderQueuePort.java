package com.ticketing.platform.application.port.out;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderQueuePort {

    Mono<Void> publish(UUID orderId);

    Flux<UUID> receive();
}
