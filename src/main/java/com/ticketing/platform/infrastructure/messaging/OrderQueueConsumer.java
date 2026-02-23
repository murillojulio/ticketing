package com.ticketing.platform.infrastructure.messaging;

import com.ticketing.platform.application.port.in.OrderProcessingUseCase;
import com.ticketing.platform.application.port.out.OrderQueuePort;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class OrderQueueConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderQueueConsumer.class);

    private final OrderQueuePort orderQueuePort;
    private final OrderProcessingUseCase orderProcessingUseCase;
    private final ApplicationProperties applicationProperties;

    private Disposable consumerSubscription;

    public OrderQueueConsumer(
        OrderQueuePort orderQueuePort,
        OrderProcessingUseCase orderProcessingUseCase,
        ApplicationProperties applicationProperties
    ) {
        this.orderQueuePort = orderQueuePort;
        this.orderProcessingUseCase = orderProcessingUseCase;
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    public void start() {
        consumerSubscription = orderQueuePort.receive()
            .flatMap(orderId -> orderProcessingUseCase.processOrder(orderId)
                .retryWhen(
                    Retry.backoff(
                        applicationProperties.getQueueRetryAttempts(),
                        Duration.ofMillis(120)
                    )
                )
                .onErrorResume(error -> {
                    LOGGER.error("Asynchronous order processing failed for order {}", orderId, error);
                    return Mono.empty();
                }), applicationProperties.getConsumerConcurrency())
            .subscribe();
    }

    @PreDestroy
    public void stop() {
        if (consumerSubscription != null) {
            consumerSubscription.dispose();
        }
    }
}
