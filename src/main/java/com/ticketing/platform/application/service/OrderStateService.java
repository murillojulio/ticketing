package com.ticketing.platform.application.service;

import com.ticketing.platform.application.port.out.OrderRepository;
import com.ticketing.platform.domain.exception.NotFoundException;
import com.ticketing.platform.domain.exception.OptimisticLockingConflictException;
import com.ticketing.platform.domain.model.Order;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class OrderStateService {

    private final OrderRepository orderRepository;

    public OrderStateService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Mono<Order> transition(
        UUID orderId,
        Predicate<Order> precondition,
        Function<Order, Order> transition
    ) {
        return Mono.defer(() -> orderRepository.findById(orderId)
            .switchIfEmpty(Mono.error(new NotFoundException("Order not found: " + orderId)))
            .flatMap(current -> {
                if (!precondition.test(current)) {
                    return Mono.just(current);
                }
                Order updated = transition.apply(current);
                return orderRepository.compareAndSet(orderId, current.version(), updated)
                    .flatMap(success -> success
                        ? Mono.just(updated)
                        : Mono.error(new OptimisticLockingConflictException("Order update conflict")));
            }))
            .retryWhen(
                Retry.fixedDelay(20, Duration.ofMillis(15))
                    .filter(OptimisticLockingConflictException.class::isInstance)
            );
    }
}
