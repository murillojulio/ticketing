package com.ticketing.platform.application.service;

import com.ticketing.platform.application.model.PaymentEvent;
import com.ticketing.platform.application.port.in.PaymentEventProcessingUseCase;
import com.ticketing.platform.application.port.out.ClockPort;
import com.ticketing.platform.application.port.out.OrderRepository;
import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.util.Set;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PaymentEventProcessingService implements PaymentEventProcessingUseCase {

    private static final Set<TicketState> RELEASABLE_STATES = Set.of(
        TicketState.RESERVED,
        TicketState.PENDING_CONFIRMATION
    );

    private final OrderRepository orderRepository;
    private final OrderStateService orderStateService;
    private final InventoryService inventoryService;
    private final ClockPort clockPort;

    public PaymentEventProcessingService(
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
    public Mono<Void> processPaymentEvent(PaymentEvent paymentEvent) {
        return orderRepository.findById(paymentEvent.orderId())
            .flatMap(order -> switch (paymentEvent.status()) {
                case CONFIRMED -> processPaymentConfirmation(order, paymentEvent);
                case FAILED -> processPaymentFailure(order, paymentEvent);
            })
            .then();
    }

    private Mono<Void> processPaymentConfirmation(Order order, PaymentEvent paymentEvent) {
        if (order.state() == TicketState.SOLD || order.state() == TicketState.COMPLIMENTARY) {
            return Mono.empty();
        }
        if (order.state() == TicketState.AVAILABLE) {
            return Mono.empty();
        }

        return ensurePendingConfirmation(order, paymentEvent)
            .flatMap(pendingOrder -> {
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
                            "Payment confirmed: " + paymentEvent.paymentId()
                        )
                    ))
                    .then();
            });
    }

    private Mono<Order> ensurePendingConfirmation(Order order, PaymentEvent paymentEvent) {
        if (order.state() == TicketState.PENDING_CONFIRMATION) {
            return Mono.just(order);
        }
        if (order.state() != TicketState.RESERVED) {
            return Mono.just(order);
        }

        return orderStateService.transition(
            order.id(),
            current -> current.state() == TicketState.RESERVED,
            current -> current.transitionTo(
                TicketState.PENDING_CONFIRMATION,
                clockPort.now(),
                "Payment confirmed before async preparation: " + paymentEvent.paymentId()
            )
        );
    }

    private Mono<Void> processPaymentFailure(Order order, PaymentEvent paymentEvent) {
        if (!RELEASABLE_STATES.contains(order.state())) {
            return Mono.empty();
        }

        return orderStateService.transition(
            order.id(),
            current -> RELEASABLE_STATES.contains(current.state()),
            current -> current.transitionTo(
                TicketState.AVAILABLE,
                clockPort.now(),
                "Payment failed: " + paymentEvent.paymentId()
            )
        ).flatMap(updatedOrder -> {
            if (updatedOrder.state() != TicketState.AVAILABLE) {
                return Mono.empty();
            }
            return inventoryService.releaseReservation(updatedOrder.eventId(), updatedOrder.quantity()).then();
        });
    }
}
