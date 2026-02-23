package com.ticketing.platform.infrastructure.web.dto;

import com.ticketing.platform.domain.model.AuditEntry;
import com.ticketing.platform.domain.model.TicketState;
import java.time.Instant;

public record AuditEntryResponse(
    TicketState fromState,
    TicketState toState,
    Instant changedAt,
    String reason
) {

    public static AuditEntryResponse from(AuditEntry entry) {
        return new AuditEntryResponse(
            entry.fromState(),
            entry.toState(),
            entry.changedAt(),
            entry.reason()
        );
    }
}
