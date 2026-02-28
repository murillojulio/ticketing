package com.ticketing.platform.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.exception.InsufficientInventoryException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    void shouldCreateEventWithAllTicketsAvailable() {
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Main venue",
            100
        );

        assertThat(event.availableTickets()).isEqualTo(100);
        assertThat(event.reservedTickets()).isZero();
        assertThat(event.soldTickets()).isZero();
        assertThat(event.complimentaryTickets()).isZero();
        assertThat(event.version()).isZero();
    }

    @Test
    void shouldReserveTicketsWhenInventoryIsAvailable() {
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Main venue",
            20
        );

        Event updated = event.reserve(5);

        assertThat(updated.availableTickets()).isEqualTo(15);
        assertThat(updated.reservedTickets()).isEqualTo(5);
        assertThat(updated.version()).isEqualTo(1);
    }

    @Test
    void shouldFailWhenReservingMoreThanAvailable() {
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Main venue",
            3
        );

        assertThatThrownBy(() -> event.reserve(4))
            .isInstanceOf(InsufficientInventoryException.class)
            .hasMessageContaining("Not enough available tickets");
    }

    @Test
    void shouldConfirmSaleFromReservedInventory() {
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Main venue",
            10
        ).reserve(6);

        Event updated = event.confirmSale(4);

        assertThat(updated.availableTickets()).isEqualTo(4);
        assertThat(updated.reservedTickets()).isEqualTo(2);
        assertThat(updated.soldTickets()).isEqualTo(4);
    }

    @Test
    void shouldReleaseReservedTicketsBackToAvailable() {
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Main venue",
            12
        ).reserve(7);

        Event updated = event.releaseReservation(3);

        assertThat(updated.availableTickets()).isEqualTo(8);
        assertThat(updated.reservedTickets()).isEqualTo(4);
    }

    @Test
    void shouldRejectInvalidTicketCounterComposition() {
        UUID eventId = UUID.randomUUID();

        assertThatThrownBy(() -> new Event(
            eventId,
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Main venue",
            10,
            9,
            0,
            0,
            0,
            0
        )).isInstanceOf(DomainException.class)
            .hasMessageContaining("add up");
    }

    @Test
    void shouldGrantComplimentaryTicketsWhenAvailable() {
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Main venue",
            10
        );

        Event updated = event.grantComplimentary(3);

        assertThat(updated.availableTickets()).isEqualTo(7);
        assertThat(updated.complimentaryTickets()).isEqualTo(3);
    }

    @Test
    void shouldRejectComplimentaryAllocationWhenInsufficientAvailability() {
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Main venue",
            2
        );

        assertThatThrownBy(() -> event.grantComplimentary(3))
            .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void shouldFailWhenReleasingMoreThanReserved() {
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Main venue",
            5
        ).reserve(2);

        assertThatThrownBy(() -> event.releaseReservation(3))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Cannot release more tickets");
    }

    @Test
    void shouldFailWhenConfirmingSaleWithoutEnoughReservedTickets() {
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Main venue",
            6
        ).reserve(1);

        assertThatThrownBy(() -> event.confirmSale(2))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Cannot confirm sale");
    }

    @Test
    void shouldRejectZeroQuantityOperations() {
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Main venue",
            6
        );

        assertThatThrownBy(() -> event.reserve(0))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Quantity must be greater than zero");
    }
}
