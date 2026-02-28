package com.ticketing.platform.infrastructure.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.platform.domain.model.Event;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventDocumentTest {

    @Test
    void shouldMapDomainToDocumentAndBack() {
        Event domain = Event.create(
            UUID.randomUUID(),
            "Concert",
            Instant.parse("2026-10-10T20:00:00Z"),
            "Arena",
            100
        ).reserve(15);

        EventDocument document = EventDocument.fromDomain(domain);
        Event restored = document.toDomain();

        assertThat(document.id()).isEqualTo(domain.id().toString());
        assertThat(restored).isEqualTo(domain);
    }
}
