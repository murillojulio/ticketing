package com.ticketing.platform.infrastructure.persistence.mongo;

import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("orders")
public record OrderDocument(
    @Id String id,
    String eventId,
    String customerId,
    int quantity,
    TicketState state,
    Instant createdAt,
    Instant expiresAt,
    long version,
    List<AuditEntryDocument> auditTrail
) {

    public static OrderDocument fromDomain(Order order) {
        return new OrderDocument(
            order.id().toString(),
            order.eventId().toString(),
            order.customerId(),
            order.quantity(),
            order.state(),
            order.createdAt(),
            order.expiresAt(),
            order.version(),
            order.auditTrail().stream().map(AuditEntryDocument::fromDomain).toList()
        );
    }

    public Order toDomain() {
        return new Order(
            UUID.fromString(id),
            UUID.fromString(eventId),
            customerId,
            quantity,
            state,
            createdAt,
            expiresAt,
            version,
            auditTrail == null ? List.of() : auditTrail.stream().map(AuditEntryDocument::toDomain).toList()
        );
    }
}
