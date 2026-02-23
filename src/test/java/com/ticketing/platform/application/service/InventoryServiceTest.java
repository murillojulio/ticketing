package com.ticketing.platform.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.port.out.EventRepository;
import com.ticketing.platform.domain.exception.NotFoundException;
import com.ticketing.platform.domain.model.Event;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Test
    void shouldRetryOnOptimisticConflict() {
        InventoryService service = new InventoryService(eventRepository);
        UUID eventId = UUID.randomUUID();
        Event current = Event.create(eventId, "Festival", Instant.parse("2026-05-01T10:00:00Z"), "Arena", 10);

        when(eventRepository.findById(eventId)).thenReturn(Mono.just(current), Mono.just(current));
        when(eventRepository.compareAndSet(eventId, 0, current.reserve(2)))
            .thenReturn(Mono.just(false), Mono.just(true));

        Event updated = service.reserveTickets(eventId, 2).block();

        assertThat(updated).isNotNull();
        assertThat(updated.reservedTickets()).isEqualTo(2);
        assertThat(updated.availableTickets()).isEqualTo(8);
    }

    @Test
    void shouldFailWhenEventDoesNotExist() {
        InventoryService service = new InventoryService(eventRepository);
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> service.reserveTickets(eventId, 1).block())
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Event not found");
    }
}
