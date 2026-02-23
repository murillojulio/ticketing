package com.ticketing.platform.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.platform.application.port.in.OrderProcessingUseCase;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class OrderQueueConsumerTest {

    @Test
    void shouldConsumeMessagesAndInvokeProcessingUseCase() throws InterruptedException {
        ReactorOrderQueueAdapter queue = new ReactorOrderQueueAdapter();
        ApplicationProperties properties = new ApplicationProperties();
        properties.setConsumerConcurrency(1);
        properties.setQueueRetryAttempts(1);

        AtomicReference<UUID> processedOrderId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        OrderProcessingUseCase useCase = orderId -> {
            processedOrderId.set(orderId);
            latch.countDown();
            return Mono.empty();
        };

        OrderQueueConsumer consumer = new OrderQueueConsumer(queue, useCase, properties);
        consumer.start();

        UUID orderId = UUID.randomUUID();
        queue.publish(orderId).block();

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        consumer.stop();

        assertThat(completed).isTrue();
        assertThat(processedOrderId.get()).isEqualTo(orderId);
    }
}
