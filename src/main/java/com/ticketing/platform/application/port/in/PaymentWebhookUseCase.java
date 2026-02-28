package com.ticketing.platform.application.port.in;

import com.ticketing.platform.application.model.PaymentEvent;
import reactor.core.publisher.Mono;

public interface PaymentWebhookUseCase {

    Mono<Void> publishPaymentEvent(PaymentEvent paymentEvent);
}
