package com.ticketing.platform.infrastructure.persistence.inmemory;

import com.ticketing.platform.application.port.out.EventRepository;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.model.Event;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class InMemoryEventRepository implements EventRepository {

    private final ConcurrentMap<UUID, Event> events = new ConcurrentHashMap<>();

    @Override
    public Mono<Event> create(Event event) {
        Event previous = events.putIfAbsent(event.id(), event);
        if (previous != null) {
            return Mono.error(new DomainException("Event already exists: " + event.id()));
        }
        return Mono.just(event);
    }

    @Override
    public Mono<Event> findById(UUID eventId) {
        return Mono.justOrEmpty(events.get(eventId));
    }

    @Override
    public Flux<Event> findAll() {
        return Flux.fromIterable(events.values());
    }

    @Override
    public Mono<Boolean> compareAndSet(UUID eventId, long expectedVersion, Event updatedEvent) {
        AtomicBoolean updated = new AtomicBoolean(false);
        events.computeIfPresent(eventId, (ignored, current) -> {
            if (current.version() == expectedVersion) {
                updated.set(true);
                return updatedEvent;
            }
            return current;
        });
        return Mono.just(updated.get());
    }
}
