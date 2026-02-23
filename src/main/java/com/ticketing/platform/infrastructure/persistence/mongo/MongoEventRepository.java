package com.ticketing.platform.infrastructure.persistence.mongo;

import com.ticketing.platform.application.port.out.EventRepository;
import com.ticketing.platform.domain.model.Event;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@ConditionalOnProperty(prefix = "ticketing.adapters", name = "persistence", havingValue = "mongo", matchIfMissing = true)
public class MongoEventRepository implements EventRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoEventRepository(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Event> create(Event event) {
        return mongoTemplate.insert(EventDocument.fromDomain(event))
            .map(EventDocument::toDomain);
    }

    @Override
    public Mono<Event> findById(UUID eventId) {
        return mongoTemplate.findById(eventId.toString(), EventDocument.class)
            .map(EventDocument::toDomain);
    }

    @Override
    public Flux<Event> findAll() {
        return mongoTemplate.findAll(EventDocument.class)
            .map(EventDocument::toDomain);
    }

    @Override
    public Mono<Boolean> compareAndSet(UUID eventId, long expectedVersion, Event updatedEvent) {
        Query query = Query.query(
            Criteria.where("_id").is(eventId.toString())
                .and("version").is(expectedVersion)
        );
        return mongoTemplate.findAndReplace(
                query,
                EventDocument.fromDomain(updatedEvent),
                FindAndReplaceOptions.options().returnNew()
            )
            .hasElement();
    }
}
