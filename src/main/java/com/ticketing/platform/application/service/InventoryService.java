package com.ticketing.platform.application.service;

import com.ticketing.platform.application.port.out.EventRepository;
import com.ticketing.platform.domain.exception.NotFoundException;
import com.ticketing.platform.domain.exception.OptimisticLockingConflictException;
import com.ticketing.platform.domain.model.Event;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class InventoryService {

    private final EventRepository eventRepository;

    public InventoryService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Mono<Event> reserveTickets(UUID eventId, int quantity) {
        return mutateEvent(eventId, event -> event.reserve(quantity));
    }

    public Mono<Event> releaseReservation(UUID eventId, int quantity) {
        return mutateEvent(eventId, event -> event.releaseReservation(quantity));
    }

    public Mono<Event> confirmSale(UUID eventId, int quantity) {
        return mutateEvent(eventId, event -> event.confirmSale(quantity));
    }

    private Mono<Event> mutateEvent(UUID eventId, Function<Event, Event> mutation) {
        return Mono.defer(() -> eventRepository.findById(eventId)
            .switchIfEmpty(Mono.error(new NotFoundException("Event not found: " + eventId)))
            .flatMap(current -> {
                Event updated = mutation.apply(current);
                return eventRepository.compareAndSet(eventId, current.version(), updated)
                    .flatMap(success -> success
                        ? Mono.just(updated)
                        : Mono.error(new OptimisticLockingConflictException("Event update conflict")));
            }))
            .retryWhen(
                Retry.fixedDelay(20, Duration.ofMillis(15))
                    .filter(OptimisticLockingConflictException.class::isInstance)
            );
    }
}
