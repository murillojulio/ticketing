package com.ticketing.platform.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.exception.InvalidStateTransitionException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void shouldCreateReservedOrderWithAuditTrail() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        Order order = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            2,
            now,
            Duration.ofMinutes(10)
        );

        assertThat(order.state()).isEqualTo(TicketState.RESERVED);
        assertThat(order.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(10)));
        assertThat(order.auditTrail()).hasSize(1);
        assertThat(order.auditTrail().getFirst().toState()).isEqualTo(TicketState.RESERVED);
    }

    @Test
    void shouldTransitionFromReservedToSoldThroughPending() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        Order order = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            2,
            now,
            Duration.ofMinutes(10)
        );

        Order pending = order.transitionTo(
            TicketState.PENDING_CONFIRMATION,
            now.plusSeconds(1),
            "Processing"
        );
        Order sold = pending.transitionTo(TicketState.SOLD, now.plusSeconds(2), "Confirmed");

        assertThat(sold.state()).isEqualTo(TicketState.SOLD);
        assertThat(sold.version()).isEqualTo(2);
        assertThat(sold.auditTrail()).hasSize(3);
    }

    @Test
    void shouldRejectInvalidTransition() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        Order order = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            2,
            now,
            Duration.ofMinutes(10)
        ).transitionTo(
            TicketState.PENDING_CONFIRMATION,
            now.plusSeconds(1),
            "Processing"
        ).transitionTo(
            TicketState.SOLD,
            now.plusSeconds(2),
            "Confirmed"
        );

        assertThatThrownBy(() -> order.transitionTo(
            TicketState.AVAILABLE,
            now.plusSeconds(3),
            "Rollback"
        )).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void shouldDetectExpiredOrder() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        Order order = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            1,
            now,
            Duration.ofMinutes(5)
        );

        assertThat(order.isExpired(now.plus(Duration.ofMinutes(6)))).isTrue();
        assertThat(order.isExpired(now.plus(Duration.ofMinutes(1)))).isFalse();
    }

    @Test
    void shouldReturnSameInstanceWhenTransitioningToSameState() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        Order order = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            1,
            now,
            Duration.ofMinutes(5)
        );

        Order unchanged = order.transitionTo(TicketState.RESERVED, now.plusSeconds(5), "No-op");

        assertThat(unchanged).isSameAs(order);
    }

    @Test
    void shouldAllowAvailableToComplimentaryTransition() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        Order order = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            1,
            now,
            Duration.ofMinutes(5)
        );

        Order available = order.transitionTo(TicketState.AVAILABLE, now.plusSeconds(30), "Expired");
        Order complimentary = available.transitionTo(TicketState.COMPLIMENTARY, now.plusSeconds(31), "Courtesy");

        assertThat(complimentary.state()).isEqualTo(TicketState.COMPLIMENTARY);
        assertThat(complimentary.isFinalState()).isTrue();
    }

    @Test
    void shouldValidateHoldDurationOnReservationCreation() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        assertThatThrownBy(() -> Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            1,
            now,
            Duration.ZERO
        )).isInstanceOf(DomainException.class)
            .hasMessageContaining("hold duration");
    }
}
