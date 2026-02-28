package com.ticketing.platform.infrastructure.messaging;

import com.ticketing.platform.application.model.PaymentEvent;
import com.ticketing.platform.application.port.out.PaymentEventQueuePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
@ConditionalOnProperty(prefix = "ticketing.adapters", name = "queue", havingValue = "inmemory")
public class ReactorPaymentEventQueueAdapter implements PaymentEventQueuePort {

    private final Sinks.Many<PaymentEvent> sink = Sinks.many().unicast().onBackpressureBuffer();

    @Override
    public Mono<Void> publish(PaymentEvent paymentEvent) {
        Sinks.EmitResult result = sink.tryEmitNext(paymentEvent);
        if (result.isFailure()) {
            return Mono.error(new IllegalStateException("Unable to publish payment event. Cause: " + result));
        }
        return Mono.empty();
    }

    @Override
    public Flux<PaymentEventMessage> receive() {
        return sink.asFlux()
            .map(paymentEvent -> new PaymentEventMessage(paymentEvent, Mono.empty()));
    }
}
