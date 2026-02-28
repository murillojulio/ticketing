package com.ticketing.platform.application.service;

import com.ticketing.platform.application.port.in.ReservationReleaseUseCase;
import com.ticketing.platform.application.port.out.ClockPort;
import com.ticketing.platform.application.port.out.OrderRepository;
import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.util.Set;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ReservationReleaseService implements ReservationReleaseUseCase {

    private static final Set<TicketState> EXPIRABLE_STATES = Set.of(
        TicketState.RESERVED,
        TicketState.PENDING_CONFIRMATION
    );

    private final OrderRepository orderRepository;
    private final OrderStateService orderStateService;
    private final InventoryService inventoryService;
    private final ClockPort clockPort;

    public ReservationReleaseService(
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
    public Mono<Void> releaseExpiredReservations() {
        return orderRepository.findByStates(EXPIRABLE_STATES)
            .concatMap(this::releaseIfExpired)
            .then();
    }

    private Mono<Void> releaseIfExpired(Order order) {
        if (!order.isExpired(clockPort.now())) {
            return Mono.empty();
        }
        return orderStateService.transition(
            order.id(),
            current -> EXPIRABLE_STATES.contains(current.state()) && current.isExpired(clockPort.now()),
            current -> current.transitionTo(
                TicketState.AVAILABLE,
                clockPort.now(),
                "Reservation expired and inventory restored"
            )
        ).flatMap(updatedOrder -> {
            if (updatedOrder.state() != TicketState.AVAILABLE) {
                return Mono.empty();
            }
            return inventoryService.releaseReservation(updatedOrder.eventId(), updatedOrder.quantity()).then();
        });
    }
}
