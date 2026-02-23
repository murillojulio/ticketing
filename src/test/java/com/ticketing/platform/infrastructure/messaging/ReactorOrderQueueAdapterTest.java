package com.ticketing.platform.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class ReactorOrderQueueAdapterTest {

    @Test
    void shouldPublishAndReceiveOrderIds() {
        ReactorOrderQueueAdapter queue = new ReactorOrderQueueAdapter();
        UUID orderId = UUID.randomUUID();

        StepVerifier.create(queue.receive().take(1))
            .then(() -> queue.publish(orderId).block())
            .expectNext(orderId)
            .expectComplete()
            .verify(Duration.ofSeconds(2));
    }

    @Test
    void shouldAllowMultipleMessages() {
        ReactorOrderQueueAdapter queue = new ReactorOrderQueueAdapter();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        StepVerifier.create(queue.receive().take(2))
            .then(() -> {
                queue.publish(first).block();
                queue.publish(second).block();
            })
            .expectNext(first, second)
            .verifyComplete();

        assertThat(first).isNotEqualTo(second);
    }
}
