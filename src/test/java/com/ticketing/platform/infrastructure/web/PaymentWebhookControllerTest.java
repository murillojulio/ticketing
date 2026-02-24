package com.ticketing.platform.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.port.in.PaymentWebhookUseCase;
import com.ticketing.platform.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(
    controllers = PaymentWebhookController.class,
    excludeAutoConfiguration = {
        ReactiveSecurityAutoConfiguration.class,
        ReactiveUserDetailsServiceAutoConfiguration.class
    }
)
@Import(GlobalExceptionHandler.class)
class PaymentWebhookControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentWebhookUseCase paymentWebhookUseCase;

    @Test
    void shouldAcceptPaymentWebhook() {
        when(paymentWebhookUseCase.publishPaymentEvent(any())).thenReturn(Mono.empty());

        webTestClient.post()
            .uri("/api/payments/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "orderId": "11111111-1111-1111-1111-111111111111",
                  "paymentId": "payment-1",
                  "status": "CONFIRMED",
                  "occurredAt": "2026-02-01T10:00:00Z"
                }
                """)
            .exchange()
            .expectStatus().isAccepted();
    }

    @Test
    void shouldValidateWebhookPayload() {
        webTestClient.post()
            .uri("/api/payments/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "orderId": null,
                  "paymentId": "",
                  "status": null
                }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void shouldMapDomainExceptionToBadRequest() {
        when(paymentWebhookUseCase.publishPaymentEvent(any()))
            .thenReturn(Mono.error(new DomainException("Invalid payment event")));

        webTestClient.post()
            .uri("/api/payments/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "orderId": "11111111-1111-1111-1111-111111111111",
                  "paymentId": "payment-1",
                  "status": "FAILED"
                }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").isEqualTo("Invalid payment event");
    }
}
