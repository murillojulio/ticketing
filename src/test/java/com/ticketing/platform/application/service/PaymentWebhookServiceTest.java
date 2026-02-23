package com.ticketing.platform.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.model.PaymentEvent;
import com.ticketing.platform.application.model.PaymentStatus;
import com.ticketing.platform.application.port.out.ClockPort;
import com.ticketing.platform.application.port.out.PaymentEventQueuePort;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock
    private PaymentEventQueuePort paymentEventQueuePort;

    @Mock
    private ClockPort clockPort;

    @Test
    void shouldPublishValidPaymentEvent() {
        ApplicationProperties properties = new ApplicationProperties();
        PaymentWebhookService service = new PaymentWebhookService(paymentEventQueuePort, clockPort, properties);
        PaymentEvent paymentEvent = new PaymentEvent(
            UUID.randomUUID(),
            "payment-1",
            PaymentStatus.CONFIRMED,
            Instant.parse("2026-02-01T10:00:00Z")
        );
        when(paymentEventQueuePort.publish(any(PaymentEvent.class))).thenReturn(Mono.empty());

        service.publishPaymentEvent(paymentEvent).block();

        verify(paymentEventQueuePort, times(1)).publish(any(PaymentEvent.class));
    }

    @Test
    void shouldUseCurrentClockWhenOccurredAtIsMissing() {
        ApplicationProperties properties = new ApplicationProperties();
        PaymentWebhookService service = new PaymentWebhookService(paymentEventQueuePort, clockPort, properties);
        Instant now = Instant.parse("2026-02-01T10:00:00Z");
        when(clockPort.now()).thenReturn(now);
        when(paymentEventQueuePort.publish(any(PaymentEvent.class))).thenReturn(Mono.empty());

        service.publishPaymentEvent(new PaymentEvent(
            UUID.randomUUID(),
            "payment-2",
            PaymentStatus.CONFIRMED,
            null
        )).block();

        verify(clockPort, times(1)).now();
        verify(paymentEventQueuePort, times(1)).publish(any(PaymentEvent.class));
    }

    @Test
    void shouldRejectInvalidWebhookPayload() {
        ApplicationProperties properties = new ApplicationProperties();
        PaymentWebhookService service = new PaymentWebhookService(paymentEventQueuePort, clockPort, properties);

        assertThatThrownBy(() -> service.publishPaymentEvent(new PaymentEvent(
            UUID.randomUUID(),
            "",
            PaymentStatus.CONFIRMED,
            Instant.parse("2026-02-01T10:00:00Z")
        )).block()).isInstanceOf(DomainException.class);
    }
}
