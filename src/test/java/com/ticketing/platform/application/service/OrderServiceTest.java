package com.ticketing.platform.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.model.CreateOrderCommand;
import com.ticketing.platform.application.port.out.ClockPort;
import com.ticketing.platform.application.port.out.OrderQueuePort;
import com.ticketing.platform.application.port.out.OrderRepository;
import com.ticketing.platform.domain.exception.NotFoundException;
import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderQueuePort orderQueuePort;

    @Mock
    private ClockPort clockPort;

    @Test
    void shouldCreateOrderAndPublishToQueue() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.setReservationHold(Duration.ofMinutes(10));
        properties.setQueueRetryAttempts(2);

        OrderService service = new OrderService(
            inventoryService,
            orderRepository,
            orderQueuePort,
            clockPort,
            properties
        );

        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-01T10:00:00Z");
        Order order = Order.reserve(
            UUID.randomUUID(),
            eventId,
            "customer-1",
            2,
            now,
            Duration.ofMinutes(10)
        );

        when(clockPort.now()).thenReturn(now);
        when(inventoryService.reserveTickets(eventId, 2)).thenReturn(Mono.empty());
        when(orderRepository.create(any(Order.class))).thenReturn(Mono.just(order));
        when(orderQueuePort.publish(order.id())).thenReturn(Mono.empty());

        Order created = service.createOrder(new CreateOrderCommand(eventId, "customer-1", 2)).block();

        assertThat(created).isNotNull();
        assertThat(created.eventId()).isEqualTo(eventId);
        assertThat(created.quantity()).isEqualTo(2);
    }

    @Test
    void shouldReturnNotFoundForMissingOrder() {
        ApplicationProperties properties = new ApplicationProperties();
        OrderService service = new OrderService(
            inventoryService,
            orderRepository,
            orderQueuePort,
            clockPort,
            properties
        );
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> service.getOrder(orderId).block())
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Order not found");
    }
}
