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

class OrderProcessingServiceTest {

    @Test
    void shouldProcessReservedOrderAndMarkItAsPendingConfirmation() {
        InMemoryEventRepository eventRepository = new InMemoryEventRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        MutableClock clock = new MutableClock(Instant.parse("2026-02-01T10:00:00Z"));
        InventoryService inventoryService = new InventoryService(eventRepository);
        OrderStateService orderStateService = new OrderStateService(orderRepository);
        OrderProcessingService service = new OrderProcessingService(
            orderRepository,
            orderStateService,
            inventoryService,
            clock
        );

        Event event = Event.create(UUID.randomUUID(), "Festival", clock.now().plusSeconds(3600), "Arena", 10);
        eventRepository.create(event).block();
        inventoryService.reserveTickets(event.id(), 3).block();

        Order order = Order.reserve(
            UUID.randomUUID(),
            event.id(),
            "customer-1",
            3,
            clock.now(),
            Duration.ofMinutes(10)
        );
        orderRepository.create(order).block();

        service.processOrder(order.id()).block();

        Order updatedOrder = orderRepository.findById(order.id()).block();
        Event updatedEvent = eventRepository.findById(event.id()).block();

        assertThat(updatedOrder.state()).isEqualTo(TicketState.PENDING_CONFIRMATION);
        assertThat(updatedEvent.soldTickets()).isZero();
        assertThat(updatedEvent.reservedTickets()).isEqualTo(3);
    }

    @Test
    void shouldReleaseExpiredOrderDuringProcessing() {
        InMemoryEventRepository eventRepository = new InMemoryEventRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        MutableClock clock = new MutableClock(Instant.parse("2026-02-01T10:00:00Z"));
        InventoryService inventoryService = new InventoryService(eventRepository);
        OrderStateService orderStateService = new OrderStateService(orderRepository);
        OrderProcessingService service = new OrderProcessingService(
            orderRepository,
            orderStateService,
            inventoryService,
            clock
        );

        Event event = Event.create(UUID.randomUUID(), "Festival", clock.now().plusSeconds(3600), "Arena", 6);
        eventRepository.create(event).block();
        inventoryService.reserveTickets(event.id(), 2).block();

        Order order = Order.reserve(
            UUID.randomUUID(),
            event.id(),
            "customer-2",
            2,
            clock.now(),
            Duration.ofMinutes(1)
        );
        orderRepository.create(order).block();

        clock.setCurrent(clock.now().plus(Duration.ofMinutes(2)));

        service.processOrder(order.id()).block();

        Order updatedOrder = orderRepository.findById(order.id()).block();
        Event updatedEvent = eventRepository.findById(event.id()).block();

        assertThat(updatedOrder.state()).isEqualTo(TicketState.AVAILABLE);
        assertThat(updatedEvent.availableTickets()).isEqualTo(6);
        assertThat(updatedEvent.reservedTickets()).isZero();
        assertThat(updatedEvent.soldTickets()).isZero();
    }
}
