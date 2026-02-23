package com.ticketing.platform.infrastructure.messaging;

import com.ticketing.platform.application.port.out.OrderQueuePort;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
public class ReactorOrderQueueAdapter implements OrderQueuePort {

    private final Sinks.Many<UUID> sink = Sinks.many().unicast().onBackpressureBuffer();

    @Override
    public Mono<Void> publish(UUID orderId) {
        Sinks.EmitResult result = sink.tryEmitNext(orderId);
        if (result.isFailure()) {
            return Mono.error(new IllegalStateException("Unable to publish order message. Cause: " + result));
        }
        return Mono.empty();
    }

    @Override
    public Flux<UUID> receive() {
        return sink.asFlux();
    }
}
