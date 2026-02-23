package com.ticketing.platform.infrastructure.web.dto;

import com.ticketing.platform.domain.model.Event;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(
    UUID id,
    String name,
    Instant date,
    String venue,
    int totalCapacity,
    int availableTickets,
    int reservedTickets,
    int soldTickets,
    int complimentaryTickets
) {

    public static EventResponse from(Event event) {
        return new EventResponse(
            event.id(),
            event.name(),
            event.date(),
            event.venue(),
            event.totalCapacity(),
            event.availableTickets(),
            event.reservedTickets(),
            event.soldTickets(),
            event.complimentaryTickets()
        );
    }
}
