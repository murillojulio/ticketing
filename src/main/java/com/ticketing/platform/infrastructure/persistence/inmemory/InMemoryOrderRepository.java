package com.ticketing.platform.infrastructure.persistence.inmemory;

import com.ticketing.platform.application.port.out.OrderRepository;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@ConditionalOnProperty(prefix = "ticketing.adapters", name = "persistence", havingValue = "inmemory")
public class InMemoryOrderRepository implements OrderRepository {

    private final ConcurrentMap<UUID, Order> orders = new ConcurrentHashMap<>();

    @Override
    public Mono<Order> create(Order order) {
        Order previous = orders.putIfAbsent(order.id(), order);
        if (previous != null) {
            return Mono.error(new DomainException("Order already exists: " + order.id()));
        }
        return Mono.just(order);
    }

    @Override
    public Mono<Order> findById(UUID orderId) {
        return Mono.justOrEmpty(orders.get(orderId));
    }

    @Override
    public Flux<Order> findByStates(Set<TicketState> states) {
        return Flux.fromIterable(orders.values())
            .filter(order -> states.contains(order.state()));
    }

    @Override
    public Mono<Boolean> compareAndSet(UUID orderId, long expectedVersion, Order updatedOrder) {
        AtomicBoolean updated = new AtomicBoolean(false);
        orders.computeIfPresent(orderId, (ignored, current) -> {
            if (current.version() == expectedVersion) {
                updated.set(true);
                return updatedOrder;
            }
            return current;
        });
        return Mono.just(updated.get());
    }
}
