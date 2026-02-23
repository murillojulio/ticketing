package com.ticketing.platform.domain.model;

import java.time.Instant;

public record AuditEntry(
    TicketState fromState,
    TicketState toState,
    Instant changedAt,
    String reason
) {
}
