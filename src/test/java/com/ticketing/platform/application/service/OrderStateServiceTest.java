package com.ticketing.platform.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.port.out.OrderRepository;
import com.ticketing.platform.domain.exception.NotFoundException;
import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class OrderStateServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Test
    void shouldTransitionOrderWhenPreconditionPasses() {
        OrderStateService service = new OrderStateService(orderRepository);
        UUID orderId = UUID.randomUUID();
        Order current = Order.reserve(
            orderId,
            UUID.randomUUID(),
            "customer-1",
            1,
            Instant.parse("2026-01-01T00:00:00Z"),
            Duration.ofMinutes(10)
        );

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(current));
        when(orderRepository.compareAndSet(any(), any(Long.class), any(Order.class))).thenReturn(Mono.just(true));

        Order updated = service.transition(
            orderId,
            order -> order.state() == TicketState.RESERVED,
            order -> order.transitionTo(
                TicketState.PENDING_CONFIRMATION,
                Instant.parse("2026-01-01T00:00:01Z"),
                "Processing"
            )
        ).block();

        assertThat(updated).isNotNull();
        assertThat(updated.state()).isEqualTo(TicketState.PENDING_CONFIRMATION);
    }

    @Test
    void shouldFailWhenOrderDoesNotExist() {
        OrderStateService service = new OrderStateService(orderRepository);
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> service.transition(orderId, order -> true, order -> order).block())
            .isInstanceOf(NotFoundException.class);
    }
}
