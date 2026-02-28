package com.ticketing.platform.infrastructure.web.dto;

import com.ticketing.platform.application.model.EventAvailability;
import java.util.UUID;

public record EventAvailabilityResponse(
    UUID eventId,
    int totalCapacity,
    int availableTickets,
    int reservedTickets,
    int soldTickets,
    int complimentaryTickets
) {

    public static EventAvailabilityResponse from(EventAvailability availability) {
        return new EventAvailabilityResponse(
            availability.eventId(),
            availability.totalCapacity(),
            availability.availableTickets(),
            availability.reservedTickets(),
            availability.soldTickets(),
            availability.complimentaryTickets()
        );
    }
}
