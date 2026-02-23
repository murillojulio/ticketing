package com.ticketing.platform.infrastructure.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.platform.domain.model.Order;
import com.ticketing.platform.domain.model.TicketState;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class MongoOrderRepositoryTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @Test
    void shouldCreateAndFindOrderById() {
        MongoOrderRepository repository = new MongoOrderRepository(mongoTemplate);
        Order order = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            2,
            Instant.parse("2026-02-01T10:00:00Z"),
            Duration.ofMinutes(10)
        );
        OrderDocument document = OrderDocument.fromDomain(order);

        when(mongoTemplate.insert(any(OrderDocument.class))).thenReturn(Mono.just(document));
        when(mongoTemplate.findById(order.id().toString(), OrderDocument.class)).thenReturn(Mono.just(document));

        Order created = repository.create(order).block();
        Order found = repository.findById(order.id()).block();

        assertThat(created).isEqualTo(order);
        assertThat(found).isEqualTo(order);
    }

    @Test
    void shouldFindOrdersByStates() {
        MongoOrderRepository repository = new MongoOrderRepository(mongoTemplate);
        Order reserved = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            1,
            Instant.parse("2026-02-01T10:00:00Z"),
            Duration.ofMinutes(10)
        );

        when(mongoTemplate.find(any(), any(Class.class)))
            .thenReturn(Flux.just(OrderDocument.fromDomain(reserved)));

        Long count = repository.findByStates(Set.of(TicketState.RESERVED)).count().block();

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void shouldCompareAndSetOrderByVersion() {
        MongoOrderRepository repository = new MongoOrderRepository(mongoTemplate);
        Order current = Order.reserve(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "customer-1",
            2,
            Instant.parse("2026-02-01T10:00:00Z"),
            Duration.ofMinutes(10)
        );
        Order updated = current.transitionTo(
            TicketState.PENDING_CONFIRMATION,
            Instant.parse("2026-02-01T10:00:05Z"),
            "Processing"
        );

        when(mongoTemplate.findAndReplace(any(), any(OrderDocument.class), any()))
            .thenReturn(Mono.just(OrderDocument.fromDomain(updated)));

        Boolean success = repository.compareAndSet(current.id(), current.version(), updated).block();

        assertThat(success).isTrue();
    }
}
