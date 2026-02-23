package com.ticketing.platform.application.service;

import com.ticketing.platform.application.model.PaymentEvent;
import com.ticketing.platform.application.port.in.PaymentWebhookUseCase;
import com.ticketing.platform.application.port.out.ClockPort;
import com.ticketing.platform.application.port.out.PaymentEventQueuePort;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class PaymentWebhookService implements PaymentWebhookUseCase {

    private final PaymentEventQueuePort paymentEventQueuePort;
    private final ClockPort clockPort;
    private final ApplicationProperties applicationProperties;

    public PaymentWebhookService(
        PaymentEventQueuePort paymentEventQueuePort,
        ClockPort clockPort,
        ApplicationProperties applicationProperties
    ) {
        this.paymentEventQueuePort = paymentEventQueuePort;
        this.clockPort = clockPort;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public Mono<Void> publishPaymentEvent(PaymentEvent paymentEvent) {
        return Mono.defer(() -> {
                if (paymentEvent == null) {
                    return Mono.error(new DomainException("Payment event is required"));
                }
                if (paymentEvent.orderId() == null) {
                    return Mono.error(new DomainException("Payment event order id is required"));
                }
                if (paymentEvent.paymentId() == null || paymentEvent.paymentId().isBlank()) {
                    return Mono.error(new DomainException("Payment event payment id is required"));
                }
                if (paymentEvent.status() == null) {
                    return Mono.error(new DomainException("Payment event status is required"));
                }

                Instant occurredAt = paymentEvent.occurredAt() == null ? clockPort.now() : paymentEvent.occurredAt();
                PaymentEvent normalized = new PaymentEvent(
                    paymentEvent.orderId(),
                    paymentEvent.paymentId(),
                    paymentEvent.status(),
                    occurredAt
                );

                return paymentEventQueuePort.publish(normalized)
                    .retryWhen(
                        Retry.backoff(
                            applicationProperties.getQueueRetryAttempts(),
                            Duration.ofMillis(100)
                        )
                    );
            }
        );
    }
}
