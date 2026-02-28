package com.ticketing.platform.application.port.out;

import com.ticketing.platform.domain.model.Event;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventRepository {

    Mono<Event> create(Event event);

    Mono<Event> findById(UUID eventId);

    Flux<Event> findAll();

    Mono<Boolean> compareAndSet(UUID eventId, long expectedVersion, Event updatedEvent);
}
