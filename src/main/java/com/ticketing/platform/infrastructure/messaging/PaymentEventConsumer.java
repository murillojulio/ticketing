package com.ticketing.platform.infrastructure.messaging;

import com.ticketing.platform.application.port.in.PaymentEventProcessingUseCase;
import com.ticketing.platform.application.port.out.PaymentEventQueuePort;
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
public class PaymentEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final PaymentEventQueuePort paymentEventQueuePort;
    private final PaymentEventProcessingUseCase paymentEventProcessingUseCase;
    private final ApplicationProperties applicationProperties;

    private Disposable consumerSubscription;

    public PaymentEventConsumer(
        PaymentEventQueuePort paymentEventQueuePort,
        PaymentEventProcessingUseCase paymentEventProcessingUseCase,
        ApplicationProperties applicationProperties
    ) {
        this.paymentEventQueuePort = paymentEventQueuePort;
        this.paymentEventProcessingUseCase = paymentEventProcessingUseCase;
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    public void start() {
        consumerSubscription = paymentEventQueuePort.receive()
            .flatMap(message -> paymentEventProcessingUseCase.processPaymentEvent(message.paymentEvent())
                    .retryWhen(
                        Retry.backoff(
                            applicationProperties.getQueueRetryAttempts(),
                            Duration.ofMillis(120)
                        )
                    )
                    .then(message.acknowledge())
                    .onErrorResume(error -> {
                        LOGGER.error(
                            "Payment event processing failed for order {}",
                            message.paymentEvent().orderId(),
                            error
                        );
                        return Mono.empty();
                    }),
                applicationProperties.getConsumerConcurrency()
            )
            .subscribe();
    }

    @PreDestroy
    public void stop() {
        if (consumerSubscription != null) {
            consumerSubscription.dispose();
        }
    }
}
