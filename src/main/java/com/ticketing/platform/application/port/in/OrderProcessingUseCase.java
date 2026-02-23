package com.ticketing.platform.application.port.in;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface OrderProcessingUseCase {

    Mono<Void> processOrder(UUID orderId);
}
