package com.ticketing.platform.application.port.in;

import com.ticketing.platform.application.model.CreateEventCommand;
import com.ticketing.platform.application.model.EventAvailability;
import com.ticketing.platform.domain.model.Event;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventUseCase {

    Mono<Event> createEvent(CreateEventCommand command);

    Flux<Event> listEvents();

    Mono<EventAvailability> getAvailability(UUID eventId);
}
