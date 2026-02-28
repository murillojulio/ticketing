package com.ticketing.platform.application.model;

import java.time.Instant;

public record CreateEventCommand(
    String name,
    Instant date,
    String venue,
    int totalCapacity
) {
}
