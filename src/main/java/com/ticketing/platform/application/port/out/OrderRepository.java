package com.ticketing.platform.application.port.out;

import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.util.Set;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderRepository {

    Mono<Order> create(Order order);

    Mono<Order> findById(UUID orderId);

    Flux<Order> findByStates(Set<TicketState> states);

    Mono<Boolean> compareAndSet(UUID orderId, long expectedVersion, Order updatedOrder);
}
