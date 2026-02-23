package com.ticketing.platform.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.platform.application.model.PaymentEvent;
import com.ticketing.platform.application.model.PaymentStatus;
import com.ticketing.platform.application.port.in.PaymentEventProcessingUseCase;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class PaymentEventConsumerTest {

    @Test
    void shouldConsumePaymentEventsAndInvokeProcessingUseCase() throws InterruptedException {
        ReactorPaymentEventQueueAdapter queue = new ReactorPaymentEventQueueAdapter();
        ApplicationProperties properties = new ApplicationProperties();
        properties.setConsumerConcurrency(1);
        properties.setQueueRetryAttempts(1);

        AtomicReference<PaymentEvent> processedPaymentEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        PaymentEventProcessingUseCase useCase = paymentEvent -> {
            processedPaymentEvent.set(paymentEvent);
            latch.countDown();
            return Mono.empty();
        };

        PaymentEventConsumer consumer = new PaymentEventConsumer(queue, useCase, properties);
        consumer.start();

        PaymentEvent event = new PaymentEvent(
            UUID.randomUUID(),
            "payment-1",
            PaymentStatus.CONFIRMED,
            Instant.parse("2026-02-01T10:00:00Z")
        );
        queue.publish(event).block();

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        consumer.stop();

        assertThat(completed).isTrue();
        assertThat(processedPaymentEvent.get()).isEqualTo(event);
    }
}
