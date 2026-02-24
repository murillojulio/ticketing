package com.ticketing.platform.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.model.EventAvailability;
import com.ticketing.platform.application.port.in.EventUseCase;
import com.ticketing.platform.domain.exception.NotFoundException;
import com.ticketing.platform.domain.model.Event;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = EventController.class)
@AutoConfigureWebTestClient(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private EventUseCase eventUseCase;

    @Test
    void shouldCreateEvent() {
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-07-01T10:00:00Z"),
            "Arena",
            100
        );
        when(eventUseCase.createEvent(any())).thenReturn(Mono.just(event));

        webTestClient.post()
            .uri("/api/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "name": "Festival",
                  "date": "2026-07-01T10:00:00Z",
                  "venue": "Arena",
                  "totalCapacity": 100
                }
                """)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.name").isEqualTo("Festival")
            .jsonPath("$.availableTickets").isEqualTo(100);
    }

    @Test
    void shouldListEvents() {
        Event first = Event.create(
            UUID.randomUUID(),
            "A",
            Instant.parse("2026-07-01T10:00:00Z"),
            "Arena A",
            10
        );
        Event second = Event.create(
            UUID.randomUUID(),
            "B",
            Instant.parse("2026-07-02T10:00:00Z"),
            "Arena B",
            20
        );
        when(eventUseCase.listEvents()).thenReturn(Flux.just(first, second));

        webTestClient.get()
            .uri("/api/events")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].name").isEqualTo("A")
            .jsonPath("$[1].name").isEqualTo("B");
    }

    @Test
    void shouldReturnAvailabilityForEvent() {
        UUID eventId = UUID.randomUUID();
        when(eventUseCase.getAvailability(eventId)).thenReturn(
            Mono.just(new EventAvailability(eventId, 100, 90, 5, 5, 0))
        );

        webTestClient.get()
            .uri("/api/events/{eventId}/availability", eventId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.availableTickets").isEqualTo(90)
            .jsonPath("$.reservedTickets").isEqualTo(5);
    }

    @Test
    void shouldHandleNotFoundError() {
        UUID eventId = UUID.randomUUID();
        when(eventUseCase.getAvailability(eventId))
            .thenReturn(Mono.error(new NotFoundException("Event not found")));

        webTestClient.get()
            .uri("/api/events/{eventId}/availability", eventId)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.message").isEqualTo("Event not found");
    }

    @Test
    void shouldValidateCreateEventRequest() {
        webTestClient.post()
            .uri("/api/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "name": "",
                  "date": null,
                  "venue": "",
                  "totalCapacity": 0
                }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo(400);
    }
}
