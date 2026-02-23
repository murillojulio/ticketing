package com.ticketing.platform.application.service;

import com.ticketing.platform.application.port.in.OrderProcessingUseCase;
import com.ticketing.platform.application.port.out.ClockPort;
import com.ticketing.platform.application.port.out.OrderRepository;
import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderProcessingService implements OrderProcessingUseCase {

    private final OrderRepository orderRepository;
    private final OrderStateService orderStateService;
    private final InventoryService inventoryService;
    private final ClockPort clockPort;

    public OrderProcessingService(
        OrderRepository orderRepository,
        OrderStateService orderStateService,
        InventoryService inventoryService,
        ClockPort clockPort
    ) {
        this.orderRepository = orderRepository;
        this.orderStateService = orderStateService;
        this.inventoryService = inventoryService;
        this.clockPort = clockPort;
    }

    @Override
    public Mono<Void> processOrder(UUID orderId) {
        return orderRepository.findById(orderId)
            .flatMap(order -> {
                if (order.isFinalState() || order.state() == TicketState.AVAILABLE) {
                    return Mono.empty();
                }
                if (order.isExpired(clockPort.now())) {
                    return expireOrder(order.id()).then();
                }
                if (order.state() != TicketState.RESERVED) {
                    return Mono.empty();
                }
                return orderStateService.transition(
                    order.id(),
                    current -> current.state() == TicketState.RESERVED,
                    current -> current.transitionTo(
                        TicketState.PENDING_CONFIRMATION,
                        clockPort.now(),
                        "Order accepted for asynchronous processing"
                    )
                ).flatMap(pendingOrder -> {
                    if (pendingOrder.state() != TicketState.PENDING_CONFIRMATION) {
                        return Mono.empty();
                    }
                    return inventoryService.confirmSale(pendingOrder.eventId(), pendingOrder.quantity())
                        .then(orderStateService.transition(
                            pendingOrder.id(),
                            current -> current.state() == TicketState.PENDING_CONFIRMATION,
                            current -> current.transitionTo(
                                TicketState.SOLD,
                                clockPort.now(),
                                "Order confirmed and sold"
                            )
                        ))
                        .then();
                });
            })
            .then();
    }

    private Mono<Order> expireOrder(UUID orderId) {
        return orderStateService.transition(
            orderId,
            current -> (
                (current.state() == TicketState.RESERVED || current.state() == TicketState.PENDING_CONFIRMATION)
                    && current.isExpired(clockPort.now())
            ),
            current -> current.transitionTo(
                TicketState.AVAILABLE,
                clockPort.now(),
                "Order reservation expired during async processing"
            )
        ).flatMap(expiredOrder -> {
            if (expiredOrder.state() != TicketState.AVAILABLE) {
                return Mono.just(expiredOrder);
            }
            return inventoryService.releaseReservation(expiredOrder.eventId(), expiredOrder.quantity())
                .thenReturn(expiredOrder);
        });
    }
}
