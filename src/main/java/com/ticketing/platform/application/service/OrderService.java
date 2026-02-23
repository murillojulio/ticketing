package com.ticketing.platform.application.service;

import com.ticketing.platform.application.model.CreateOrderCommand;
import com.ticketing.platform.application.port.in.OrderUseCase;
import com.ticketing.platform.application.port.out.ClockPort;
import com.ticketing.platform.application.port.out.OrderQueuePort;
import com.ticketing.platform.application.port.out.OrderRepository;
import com.ticketing.platform.domain.exception.NotFoundException;
import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class OrderService implements OrderUseCase {

    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final OrderQueuePort orderQueuePort;
    private final ClockPort clockPort;
    private final ApplicationProperties applicationProperties;

    public OrderService(
        InventoryService inventoryService,
        OrderRepository orderRepository,
        OrderQueuePort orderQueuePort,
        ClockPort clockPort,
        ApplicationProperties applicationProperties
    ) {
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.orderQueuePort = orderQueuePort;
        this.clockPort = clockPort;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public Mono<Order> createOrder(CreateOrderCommand command) {
        return inventoryService.reserveTickets(command.eventId(), command.quantity())
            .then(Mono.defer(() -> {
                Order order = Order.reserve(
                    UUID.randomUUID(),
                    command.eventId(),
                    command.customerId(),
                    command.quantity(),
                    clockPort.now(),
                    applicationProperties.getReservationHold()
                );
                return orderRepository.create(order);
            }))
            .flatMap(order -> orderQueuePort.publish(order.id())
                .retryWhen(
                    Retry.backoff(
                        applicationProperties.getQueueRetryAttempts(),
                        Duration.ofMillis(100)
                    )
                )
                .thenReturn(order)
            );
    }

    @Override
    public Mono<Order> getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
            .switchIfEmpty(Mono.error(new NotFoundException("Order not found: " + orderId)));
    }
}
