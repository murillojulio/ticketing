package com.ticketing.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.platform.infrastructure.web.dto.AuthResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "ticketing.security.enabled=true",
        "ticketing.adapters.persistence=inmemory",
        "ticketing.adapters.queue=inmemory"
    }
)
class SecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRequireAuthenticationForProtectedEndpoint() {
        webTestClient.get()
            .uri("/api/events")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void shouldAllowPaymentWebhookWithoutAuthentication() {
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
    void shouldAccessProtectedEndpointWithJwtToken() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        AuthResponse authResponse = webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(email))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.token()).isNotBlank();

        webTestClient.get()
            .uri("/api/events")
            .headers(headers -> headers.setBearerAuth(authResponse.token()))
            .exchange()
            .expectStatus().isOk();
    }
}
