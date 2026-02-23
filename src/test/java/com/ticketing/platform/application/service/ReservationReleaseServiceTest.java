package com.ticketing.platform.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.platform.domain.model.Event;
import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import com.ticketing.platform.infrastructure.persistence.inmemory.InMemoryEventRepository;
import com.ticketing.platform.infrastructure.persistence.inmemory.InMemoryOrderRepository;
import com.ticketing.platform.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservationReleaseServiceTest {

    @Test
    void shouldReleaseOnlyExpiredOrders() {
        InMemoryEventRepository eventRepository = new InMemoryEventRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        MutableClock clock = new MutableClock(Instant.parse("2026-03-01T10:00:00Z"));
        InventoryService inventoryService = new InventoryService(eventRepository);
        OrderStateService orderStateService = new OrderStateService(orderRepository);
        ReservationReleaseService service = new ReservationReleaseService(
            orderRepository,
            orderStateService,
            inventoryService,
            clock
        );

        Event event = Event.create(UUID.randomUUID(), "Concert", clock.now().plusSeconds(3600), "Stadium", 8);
        eventRepository.create(event).block();
        inventoryService.reserveTickets(event.id(), 5).block();

        Order expiredOrder = Order.reserve(
            UUID.randomUUID(),
            event.id(),
            "expired-customer",
            2,
            clock.now(),
            Duration.ofMinutes(1)
        );
        Order activeOrder = Order.reserve(
            UUID.randomUUID(),
            event.id(),
            "active-customer",
            3,
            clock.now(),
            Duration.ofMinutes(20)
        );

        orderRepository.create(expiredOrder).block();
        orderRepository.create(activeOrder).block();

        clock.setCurrent(clock.now().plus(Duration.ofMinutes(5)));

        service.releaseExpiredReservations().block();

        Order updatedExpiredOrder = orderRepository.findById(expiredOrder.id()).block();
        Order updatedActiveOrder = orderRepository.findById(activeOrder.id()).block();
        Event updatedEvent = eventRepository.findById(event.id()).block();

        assertThat(updatedExpiredOrder.state()).isEqualTo(TicketState.AVAILABLE);
        assertThat(updatedActiveOrder.state()).isEqualTo(TicketState.RESERVED);
        assertThat(updatedEvent.availableTickets()).isEqualTo(5);
        assertThat(updatedEvent.reservedTickets()).isEqualTo(3);
    }
}
