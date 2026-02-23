package com.ticketing.platform.infrastructure.persistence.mongo;

import com.ticketing.platform.application.port.out.OrderRepository;
import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.util.Set;
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
public class MongoOrderRepository implements OrderRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoOrderRepository(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Order> create(Order order) {
        return mongoTemplate.insert(OrderDocument.fromDomain(order))
            .map(OrderDocument::toDomain);
    }

    @Override
    public Mono<Order> findById(UUID orderId) {
        return mongoTemplate.findById(orderId.toString(), OrderDocument.class)
            .map(OrderDocument::toDomain);
    }

    @Override
    public Flux<Order> findByStates(Set<TicketState> states) {
        Query query = Query.query(Criteria.where("state").in(states));
        return mongoTemplate.find(query, OrderDocument.class)
            .map(OrderDocument::toDomain);
    }

    @Override
    public Mono<Boolean> compareAndSet(UUID orderId, long expectedVersion, Order updatedOrder) {
        Query query = Query.query(
            Criteria.where("_id").is(orderId.toString())
                .and("version").is(expectedVersion)
        );
        return mongoTemplate.findAndReplace(
                query,
                OrderDocument.fromDomain(updatedOrder),
                FindAndReplaceOptions.options().returnNew()
            )
            .hasElement();
    }
}
