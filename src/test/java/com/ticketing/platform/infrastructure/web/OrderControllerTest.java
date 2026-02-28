package com.ticketing.platform.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.port.in.OrderUseCase;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.exception.NotFoundException;
import com.ticketing.platform.domain.model.Order;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
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
    controllers = OrderController.class,
    excludeAutoConfiguration = {
        ReactiveSecurityAutoConfiguration.class,
        ReactiveUserDetailsServiceAutoConfiguration.class
    }
)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderUseCase orderUseCase;

    @Test
    void shouldCreateOrder() {
        UUID eventId = UUID.randomUUID();
        Order order = Order.reserve(
            UUID.randomUUID(),
            eventId,
            "customer-1",
            2,
            Instant.parse("2026-08-01T10:00:00Z"),
            Duration.ofMinutes(10)
        );
        when(orderUseCase.createOrder(any())).thenReturn(Mono.just(order));

        webTestClient.post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "eventId": "%s",
                  "customerId": "customer-1",
                  "quantity": 2
                }
                """.formatted(eventId))
            .exchange()
            .expectStatus().isAccepted()
            .expectBody()
            .jsonPath("$.state").isEqualTo("RESERVED")
            .jsonPath("$.quantity").isEqualTo(2);
    }

    @Test
    void shouldGetOrder() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = Order.reserve(
            orderId,
            eventId,
            "customer-1",
            1,
            Instant.parse("2026-08-01T10:00:00Z"),
            Duration.ofMinutes(10)
        );
        when(orderUseCase.getOrder(orderId)).thenReturn(Mono.just(order));

        webTestClient.get()
            .uri("/api/orders/{orderId}", orderId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(orderId.toString())
            .jsonPath("$.state").isEqualTo("RESERVED");
    }

    @Test
    void shouldReturnNotFoundWhenOrderIsMissing() {
        UUID orderId = UUID.randomUUID();
        when(orderUseCase.getOrder(orderId)).thenReturn(Mono.error(new NotFoundException("Order not found")));

        webTestClient.get()
            .uri("/api/orders/{orderId}", orderId)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.message").isEqualTo("Order not found");
    }

    @Test
    void shouldValidateOrderRequestBody() {
        webTestClient.post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "eventId": null,
                  "customerId": "",
                  "quantity": 0
                }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void shouldMapDomainExceptionToBadRequest() {
        UUID eventId = UUID.randomUUID();
        when(orderUseCase.createOrder(any())).thenReturn(Mono.error(new DomainException("Business rule violation")));

        webTestClient.post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "eventId": "%s",
                  "customerId": "customer-1",
                  "quantity": 2
                }
                """.formatted(eventId))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").isEqualTo("Business rule violation");
    }
}
