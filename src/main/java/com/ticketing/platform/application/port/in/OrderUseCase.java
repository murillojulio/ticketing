package com.ticketing.platform.application.port.in;

import com.ticketing.platform.application.model.CreateOrderCommand;
import com.ticketing.platform.domain.model.Order;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface OrderUseCase {

    Mono<Order> createOrder(CreateOrderCommand command);

    Mono<Order> getOrder(UUID orderId);
}
