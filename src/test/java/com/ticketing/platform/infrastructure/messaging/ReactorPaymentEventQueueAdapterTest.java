package com.ticketing.platform.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.platform.application.model.PaymentEvent;
import com.ticketing.platform.application.model.PaymentStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class ReactorPaymentEventQueueAdapterTest {

    @Test
    void shouldPublishAndReceivePaymentEvents() {
        ReactorPaymentEventQueueAdapter queue = new ReactorPaymentEventQueueAdapter();
        PaymentEvent event = new PaymentEvent(
            UUID.randomUUID(),
            "payment-1",
            PaymentStatus.CONFIRMED,
            Instant.parse("2026-02-01T10:00:00Z")
        );

        StepVerifier.create(queue.receive().take(1))
            .then(() -> queue.publish(event).block())
            .assertNext(message -> assertThat(message.paymentEvent()).isEqualTo(event))
            .expectComplete()
            .verify(Duration.ofSeconds(2));
    }
}
