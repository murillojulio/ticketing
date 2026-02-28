package com.ticketing.platform.infrastructure.persistence.mongo;

import com.ticketing.platform.domain.model.AuditEntry;
import com.ticketing.platform.domain.model.TicketState;
import java.time.Instant;

public record AuditEntryDocument(
    TicketState fromState,
    TicketState toState,
    Instant changedAt,
    String reason
) {

    public static AuditEntryDocument fromDomain(AuditEntry auditEntry) {
        return new AuditEntryDocument(
            auditEntry.fromState(),
            auditEntry.toState(),
            auditEntry.changedAt(),
            auditEntry.reason()
        );
    }

    public AuditEntry toDomain() {
        return new AuditEntry(
            fromState,
            toState,
            changedAt,
            reason
        );
    }
}
