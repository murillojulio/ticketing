package com.ticketing.platform.infrastructure.persistence.mongo;

import com.ticketing.platform.domain.model.Event;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("events")
public record EventDocument(
    @Id String id,
    String name,
    Instant date,
    String venue,
    int totalCapacity,
    int availableTickets,
    int reservedTickets,
    int soldTickets,
    int complimentaryTickets,
    long version
) {

    public static EventDocument fromDomain(Event event) {
        return new EventDocument(
            event.id().toString(),
            event.name(),
            event.date(),
            event.venue(),
            event.totalCapacity(),
            event.availableTickets(),
            event.reservedTickets(),
            event.soldTickets(),
            event.complimentaryTickets(),
            event.version()
        );
    }

    public Event toDomain() {
        return new Event(
            UUID.fromString(id),
            name,
            date,
            venue,
            totalCapacity,
            availableTickets,
            reservedTickets,
            soldTickets,
            complimentaryTickets,
            version
        );
    }
}
