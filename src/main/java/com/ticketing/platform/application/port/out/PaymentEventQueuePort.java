package com.ticketing.platform.application.port.out;

import com.ticketing.platform.application.model.PaymentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentEventQueuePort {

    Mono<Void> publish(PaymentEvent paymentEvent);

    Flux<PaymentEventMessage> receive();

    record PaymentEventMessage(
        PaymentEvent paymentEvent,
        Mono<Void> acknowledge
    ) {
    }
}
