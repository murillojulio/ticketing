package com.ticketing.platform.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.platform.application.model.CreateEventCommand;
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
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Test
    void shouldCreateEvent() {
        EventService eventService = new EventService(eventRepository);
        Event persisted = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-07-01T10:00:00Z"),
            "Arena",
            100
        );
        when(eventRepository.create(any(Event.class))).thenReturn(Mono.just(persisted));

        Event created = eventService.createEvent(new CreateEventCommand(
            "Festival",
            Instant.parse("2026-07-01T10:00:00Z"),
            "Arena",
            100
        )).block();

        assertThat(created).isNotNull();
        assertThat(created.name()).isEqualTo("Festival");
    }

    @Test
    void shouldFailWhenEventNotFoundOnAvailabilityQuery() {
        EventService eventService = new EventService(eventRepository);
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> eventService.getAvailability(eventId).block())
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Event not found");
    }
}
