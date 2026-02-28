package com.ticketing.platform.infrastructure.persistence.inmemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.platform.domain.model.Event;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryEventRepositoryTest {

    @Test
    void shouldStoreAndUpdateEventWithOptimisticLock() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = Event.create(
            UUID.randomUUID(),
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Arena",
            10
        );
        repository.create(event).block();

        Event reserved = event.reserve(3);
        Boolean updated = repository.compareAndSet(event.id(), event.version(), reserved).block();
        Event persisted = repository.findById(event.id()).block();

        assertThat(updated).isTrue();
        assertThat(persisted.reservedTickets()).isEqualTo(3);
        assertThat(repository.compareAndSet(event.id(), 0, reserved).block()).isFalse();
    }

    @Test
    void shouldRejectDuplicatedEventIds() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        UUID eventId = UUID.randomUUID();
        Event first = Event.create(
            eventId,
            "Festival",
            Instant.parse("2026-05-01T10:00:00Z"),
            "Arena",
            5
        );
        Event second = Event.create(
            eventId,
            "Festival 2",
            Instant.parse("2026-05-02T10:00:00Z"),
            "Arena 2",
            5
        );

        repository.create(first).block();

        assertThatThrownBy(() -> repository.create(second).block())
            .isInstanceOf(RuntimeException.class);
    }
}
