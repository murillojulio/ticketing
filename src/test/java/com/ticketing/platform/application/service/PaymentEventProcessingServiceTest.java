package com.ticketing.platform.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.platform.application.model.PaymentEvent;
import com.ticketing.platform.application.model.PaymentStatus;
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

class PaymentEventProcessingServiceTest {

    @Test
    void shouldConfirmPaymentAndMarkOrderAsSold() {
        InMemoryEventRepository eventRepository = new InMemoryEventRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        MutableClock clock = new MutableClock(Instant.parse("2026-02-01T10:00:00Z"));
        InventoryService inventoryService = new InventoryService(eventRepository);
        OrderStateService orderStateService = new OrderStateService(orderRepository);
        PaymentEventProcessingService service = new PaymentEventProcessingService(
            orderRepository,
            orderStateService,
            inventoryService,
            clock
        );

        Event event = Event.create(UUID.randomUUID(), "Festival", clock.now().plusSeconds(3600), "Arena", 10);
        eventRepository.create(event).block();
        inventoryService.reserveTickets(event.id(), 2).block();

        Order order = Order.reserve(
            UUID.randomUUID(),
            event.id(),
            "customer-1",
            2,
            clock.now(),
            Duration.ofMinutes(10)
        ).transitionTo(
            TicketState.PENDING_CONFIRMATION,
            clock.now().plusSeconds(1),
            "Queued for payment"
        );
        orderRepository.create(order).block();

        PaymentEvent paymentEvent = new PaymentEvent(
            order.id(),
            "payment-1",
            PaymentStatus.CONFIRMED,
            clock.now().plusSeconds(3)
        );

        service.processPaymentEvent(paymentEvent).block();

        Order updatedOrder = orderRepository.findById(order.id()).block();
        Event updatedEvent = eventRepository.findById(event.id()).block();

        assertThat(updatedOrder.state()).isEqualTo(TicketState.SOLD);
        assertThat(updatedEvent.soldTickets()).isEqualTo(2);
        assertThat(updatedEvent.reservedTickets()).isZero();
    }

    @Test
    void shouldReleaseReservationWhenPaymentFails() {
        InMemoryEventRepository eventRepository = new InMemoryEventRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        MutableClock clock = new MutableClock(Instant.parse("2026-02-01T10:00:00Z"));
        InventoryService inventoryService = new InventoryService(eventRepository);
        OrderStateService orderStateService = new OrderStateService(orderRepository);
        PaymentEventProcessingService service = new PaymentEventProcessingService(
            orderRepository,
            orderStateService,
            inventoryService,
            clock
        );

        Event event = Event.create(UUID.randomUUID(), "Festival", clock.now().plusSeconds(3600), "Arena", 8);
        eventRepository.create(event).block();
        inventoryService.reserveTickets(event.id(), 3).block();

        Order order = Order.reserve(
            UUID.randomUUID(),
            event.id(),
            "customer-2",
            3,
            clock.now(),
            Duration.ofMinutes(10)
        );
        orderRepository.create(order).block();

        PaymentEvent paymentEvent = new PaymentEvent(
            order.id(),
            "payment-2",
            PaymentStatus.FAILED,
            clock.now().plusSeconds(2)
        );

        service.processPaymentEvent(paymentEvent).block();

        Order updatedOrder = orderRepository.findById(order.id()).block();
        Event updatedEvent = eventRepository.findById(event.id()).block();

        assertThat(updatedOrder.state()).isEqualTo(TicketState.AVAILABLE);
        assertThat(updatedEvent.availableTickets()).isEqualTo(8);
        assertThat(updatedEvent.reservedTickets()).isZero();
        assertThat(updatedEvent.soldTickets()).isZero();
    }

    @Test
    void shouldIgnoreConfirmedPaymentForSoldOrder() {
        InMemoryEventRepository eventRepository = new InMemoryEventRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        MutableClock clock = new MutableClock(Instant.parse("2026-02-01T10:00:00Z"));
        InventoryService inventoryService = new InventoryService(eventRepository);
        OrderStateService orderStateService = new OrderStateService(orderRepository);
        PaymentEventProcessingService service = new PaymentEventProcessingService(
            orderRepository,
            orderStateService,
            inventoryService,
            clock
        );

        Event event = Event.create(UUID.randomUUID(), "Festival", clock.now().plusSeconds(3600), "Arena", 5);
        eventRepository.create(event).block();
        inventoryService.reserveTickets(event.id(), 2).block();
        inventoryService.confirmSale(event.id(), 2).block();

        Order soldOrder = Order.reserve(
            UUID.randomUUID(),
            event.id(),
            "customer-3",
            2,
            clock.now(),
            Duration.ofMinutes(10)
        ).transitionTo(
            TicketState.PENDING_CONFIRMATION,
            clock.now().plusSeconds(1),
            "Queued"
        ).transitionTo(
            TicketState.SOLD,
            clock.now().plusSeconds(2),
            "Payment confirmed"
        );
        orderRepository.create(soldOrder).block();

        PaymentEvent duplicate = new PaymentEvent(
            soldOrder.id(),
            "payment-3",
            PaymentStatus.CONFIRMED,
            clock.now().plusSeconds(4)
        );

        service.processPaymentEvent(duplicate).block();

        Event updatedEvent = eventRepository.findById(event.id()).block();
        assertThat(updatedEvent.soldTickets()).isEqualTo(2);
        assertThat(updatedEvent.reservedTickets()).isZero();
    }
}
