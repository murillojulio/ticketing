package com.ticketing.platform.infrastructure.persistence.inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryOrderRepositoryTest {

    @Test
    void shouldStoreAndFilterOrdersByState() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        Order reservedOrder = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            2,
            now,
            Duration.ofMinutes(10)
        );
        Order soldOrder = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-2",
            1,
            now,
            Duration.ofMinutes(10)
        ).transitionTo(TicketState.PENDING_CONFIRMATION, now.plusSeconds(1), "Processing")
            .transitionTo(TicketState.SOLD, now.plusSeconds(2), "Confirmed");

        repository.create(reservedOrder).block();
        repository.create(soldOrder).block();

        long reservedCount = repository.findByStates(Set.of(TicketState.RESERVED)).count().block();

        assertThat(reservedCount).isEqualTo(1);
    }

    @Test
    void shouldApplyCompareAndSetForOrderVersion() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        Order order = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            1,
            now,
            Duration.ofMinutes(10)
        );
        repository.create(order).block();

        Order pending = order.transitionTo(TicketState.PENDING_CONFIRMATION, now.plusSeconds(1), "Processing");
        Boolean updated = repository.compareAndSet(order.id(), order.version(), pending).block();

        assertThat(updated).isTrue();
        assertThat(repository.compareAndSet(order.id(), order.version(), pending).block()).isFalse();
    }
}
