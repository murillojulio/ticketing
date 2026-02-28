package com.ticketing.platform.infrastructure.web;

import com.ticketing.platform.application.model.PaymentEvent;
import com.ticketing.platform.application.port.in.PaymentWebhookUseCase;
import com.ticketing.platform.infrastructure.web.dto.PaymentWebhookRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private final PaymentWebhookUseCase paymentWebhookUseCase;

    public PaymentWebhookController(PaymentWebhookUseCase paymentWebhookUseCase) {
        this.paymentWebhookUseCase = paymentWebhookUseCase;
    }

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> paymentWebhook(@Valid @RequestBody PaymentWebhookRequest request) {
        return paymentWebhookUseCase.publishPaymentEvent(
            new PaymentEvent(
                request.orderId(),
                request.paymentId(),
                request.status(),
                request.occurredAt()
            )
        );
    }
}
