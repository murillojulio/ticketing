package com.ticketing.platform.application.port.in;

import com.ticketing.platform.application.model.PaymentEvent;
import reactor.core.publisher.Mono;

public interface PaymentEventProcessingUseCase {

    Mono<Void> processPaymentEvent(PaymentEvent paymentEvent);
}
