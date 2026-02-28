package com.ticketing.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.platform.application.model.CreateEventCommand;
import com.ticketing.platform.application.model.CreateOrderCommand;
import com.ticketing.platform.application.model.EventAvailability;
import com.ticketing.platform.application.port.in.EventUseCase;
import com.ticketing.platform.application.port.in.OrderUseCase;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.exception.InsufficientInventoryException;
import com.ticketing.platform.domain.model.Event;
import com.ticketing.platform.domain.model.Order;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ConcurrencyIntegrationTest {

    @Autowired
    private EventUseCase eventUseCase;

    @Autowired
    private OrderUseCase orderUseCase;

    @Test
    void shouldPreventOversellWhenManyRequestsCompeteForSameEvent() throws InterruptedException {
        int capacity = 50;
        Event event = eventUseCase.createEvent(
            new CreateEventCommand(
                "High demand concert",
                Instant.parse("2026-12-01T20:00:00Z"),
                "Main stadium",
                capacity
            )
        ).block();

        List<Order> successfulOrders = Flux.range(0, 250)
            .flatMap(index -> orderUseCase.createOrder(
                new CreateOrderCommand(
                    event.id(),
                    "customer-" + index,
                    1
                )
            ).onErrorResume(InsufficientInventoryException.class, error -> Mono.empty())
                .onErrorResume(DomainException.class, error -> Mono.empty()), 80)
            .collectList()
            .block();

        assertThat(successfulOrders).hasSize(capacity);

        EventAvailability availability = waitForAvailability(event.id(), capacity);
        int consumedInventory = availability.soldTickets()
            + availability.reservedTickets()
            + availability.complimentaryTickets();

        assertThat(consumedInventory).isEqualTo(capacity);
        assertThat(availability.availableTickets()).isZero();
    }

    private EventAvailability waitForAvailability(UUID eventId, int expectedCapacity) throws InterruptedException {
        EventAvailability availability = null;
        for (int i = 0; i < 30; i++) {
            availability = eventUseCase.getAvailability(eventId).block();
            int consumed = availability.soldTickets()
                + availability.reservedTickets()
                + availability.complimentaryTickets();
            if (consumed == expectedCapacity) {
                return availability;
            }
            Thread.sleep(100);
        }
        return availability;
    }
}
