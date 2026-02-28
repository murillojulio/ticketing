package com.ticketing.platform.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.exception.InvalidStateTransitionException;

public record Order(
    UUID id,
    UUID eventId,
    String customerId,
    int quantity,
    TicketState state,
    Instant createdAt,
    Instant expiresAt,
    long version,
    List<AuditEntry> auditTrail
) {

    public Order {
        if (id == null) {
            throw new DomainException("Order id is required");
        }
        if (eventId == null) {
            throw new DomainException("Order event id is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new DomainException("Order customer id is required");
        }
        if (quantity <= 0) {
            throw new DomainException("Order quantity must be greater than zero");
        }
        if (state == null) {
            throw new DomainException("Order state is required");
        }
        if (createdAt == null) {
            throw new DomainException("Order creation timestamp is required");
        }
        auditTrail = auditTrail == null ? List.of() : List.copyOf(auditTrail);
    }

    public static Order reserve(UUID id, UUID eventId, String customerId, int quantity, Instant now, Duration holdDuration) {
        if (holdDuration == null || holdDuration.isNegative() || holdDuration.isZero()) {
            throw new DomainException("Reservation hold duration must be positive");
        }
        Instant expiration = now.plus(holdDuration);
        List<AuditEntry> auditEntries = List.of(
            new AuditEntry(TicketState.AVAILABLE, TicketState.RESERVED, now, "Order created and tickets reserved")
        );
        return new Order(
            id,
            eventId,
            customerId,
            quantity,
            TicketState.RESERVED,
            now,
            expiration,
            0,
            auditEntries
        );
    }

    public Order transitionTo(TicketState nextState, Instant changedAt, String reason) {
        if (nextState == state) {
            return this;
        }
        validateTransition(state, nextState);
        List<AuditEntry> updatedAudit = new ArrayList<>(auditTrail);
        updatedAudit.add(new AuditEntry(state, nextState, changedAt, reason));
        return new Order(
            id,
            eventId,
            customerId,
            quantity,
            nextState,
            createdAt,
            expiresAt,
            version + 1,
            updatedAudit
        );
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    public boolean isFinalState() {
        return state == TicketState.SOLD || state == TicketState.COMPLIMENTARY;
    }

    private static void validateTransition(TicketState current, TicketState next) {
        boolean valid = switch (current) {
            case AVAILABLE -> next == TicketState.RESERVED || next == TicketState.COMPLIMENTARY;
            case RESERVED -> next == TicketState.PENDING_CONFIRMATION || next == TicketState.AVAILABLE;
            case PENDING_CONFIRMATION -> next == TicketState.SOLD || next == TicketState.AVAILABLE;
            case SOLD, COMPLIMENTARY -> false;
        };
        if (!valid) {
            throw new InvalidStateTransitionException(
                "Invalid order state transition from " + current + " to " + next
            );
        }
    }
}
