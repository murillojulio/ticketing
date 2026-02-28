package com.ticketing.platform.infrastructure.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderDocumentTest {

    @Test
    void shouldMapOrderDomainToDocumentAndBack() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        Order domain = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            2,
            now,
            Duration.ofMinutes(10)
        ).transitionTo(
            TicketState.PENDING_CONFIRMATION,
            now.plusSeconds(5),
            "Processing"
        );

        OrderDocument document = OrderDocument.fromDomain(domain);
        Order restored = document.toDomain();

        assertThat(document.id()).isEqualTo(domain.id().toString());
        assertThat(restored).isEqualTo(domain);
    }
}
