package com.ticketing.platform.application.model;

import java.util.UUID;

public record EventAvailability(
    UUID eventId,
    int totalCapacity,
    int availableTickets,
    int reservedTickets,
    int soldTickets,
    int complimentaryTickets
) {
}
