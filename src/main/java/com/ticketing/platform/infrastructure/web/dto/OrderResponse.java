package com.ticketing.platform.infrastructure.web.dto;

import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID eventId,
    String customerId,
    int quantity,
    TicketState state,
    Instant createdAt,
    Instant expiresAt,
    List<AuditEntryResponse> auditTrail
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.id(),
            order.eventId(),
            order.customerId(),
            order.quantity(),
            order.state(),
            order.createdAt(),
            order.expiresAt(),
            order.auditTrail().stream().map(AuditEntryResponse::from).toList()
        );
    }
}
