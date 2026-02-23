package com.ticketing.platform.infrastructure.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.platform.domain.model.Event;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class MongoEventRepositoryTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @Test
    void shouldCreateAndFindEvents() {
        MongoEventRepository repository = new MongoEventRepository(mongoTemplate);
        Event event = Event.create(
            UUID.randomUUID(),
            "Concert",
            Instant.parse("2026-10-10T20:00:00Z"),
            "Arena",
            100
        );
        EventDocument document = EventDocument.fromDomain(event);

        when(mongoTemplate.insert(any(EventDocument.class))).thenReturn(Mono.just(document));
        when(mongoTemplate.findById(event.id().toString(), EventDocument.class)).thenReturn(Mono.just(document));
        when(mongoTemplate.findAll(EventDocument.class)).thenReturn(Flux.just(document));

        Event created = repository.create(event).block();
        Event found = repository.findById(event.id()).block();
        Long count = repository.findAll().count().block();

        assertThat(created).isEqualTo(event);
        assertThat(found).isEqualTo(event);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void shouldCompareAndSetEventByVersion() {
        MongoEventRepository repository = new MongoEventRepository(mongoTemplate);
        Event current = Event.create(
            UUID.randomUUID(),
            "Concert",
            Instant.parse("2026-10-10T20:00:00Z"),
            "Arena",
            20
        );
        Event updated = current.reserve(2);

        when(mongoTemplate.findAndReplace(any(), any(EventDocument.class), any()))
            .thenReturn(Mono.just(EventDocument.fromDomain(updated)));

        Boolean success = repository.compareAndSet(current.id(), current.version(), updated).block();

        assertThat(success).isTrue();
    }

    @Test
    void shouldReturnFalseWhenCompareAndSetDoesNotMatch() {
        MongoEventRepository repository = new MongoEventRepository(mongoTemplate);
        Event current = Event.create(
            UUID.randomUUID(),
            "Concert",
            Instant.parse("2026-10-10T20:00:00Z"),
            "Arena",
            20
        );
        Event updated = current.reserve(1);

        when(mongoTemplate.findAndReplace(any(), any(EventDocument.class), any()))
            .thenReturn(Mono.empty());

        Boolean success = repository.compareAndSet(current.id(), current.version(), updated).block();

        assertThat(success).isFalse();
    }
}
