package com.ticketing.platform.application.service;

import com.ticketing.platform.application.model.CreateEventCommand;
import com.ticketing.platform.application.model.EventAvailability;
import com.ticketing.platform.application.port.in.EventUseCase;
import com.ticketing.platform.application.port.out.EventRepository;
import com.ticketing.platform.domain.exception.NotFoundException;
import com.ticketing.platform.domain.model.Event;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EventService implements EventUseCase {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Mono<Event> createEvent(CreateEventCommand command) {
        Event event = Event.create(
            UUID.randomUUID(),
            command.name(),
            command.date(),
            command.venue(),
            command.totalCapacity()
        );
        return eventRepository.create(event);
    }

    @Override
    public Flux<Event> listEvents() {
        return eventRepository.findAll();
    }

    @Override
    public Mono<EventAvailability> getAvailability(UUID eventId) {
        return eventRepository.findById(eventId)
            .switchIfEmpty(Mono.error(new NotFoundException("Event not found: " + eventId)))
            .map(event -> new EventAvailability(
                event.id(),
                event.totalCapacity(),
                event.availableTickets(),
                event.reservedTickets(),
                event.soldTickets(),
                event.complimentaryTickets()
            ));
    }
}
