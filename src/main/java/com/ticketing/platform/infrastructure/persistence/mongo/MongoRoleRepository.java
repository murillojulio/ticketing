package com.ticketing.platform.infrastructure.persistence.mongo;

import com.ticketing.platform.application.port.out.RoleRepository;
import com.ticketing.platform.domain.model.Role;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@ConditionalOnProperty(prefix = "ticketing.adapters", name = "persistence", havingValue = "mongo", matchIfMissing = true)
public class MongoRoleRepository implements RoleRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoRoleRepository(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Role> create(Role role) {
        return mongoTemplate.insert(RoleDocument.fromDomain(role))
                .map(RoleDocument::toDomain);
    }

    @Override
    public Mono<Role> update(Role role) {
        return mongoTemplate.save(RoleDocument.fromDomain(role))
                .map(RoleDocument::toDomain);
    }

    @Override
    public Mono<Role> findById(UUID id) {
        return mongoTemplate.findById(id.toString(), RoleDocument.class)
                .map(RoleDocument::toDomain);
    }

    @Override
    public Mono<Role> findByName(String name) {
        Query query = Query.query(Criteria.where("name").is(name));
        return mongoTemplate.findOne(query, RoleDocument.class)
                .map(RoleDocument::toDomain);
    }

    @Override
    public Flux<Role> findAll() {
        return mongoTemplate.findAll(RoleDocument.class)
                .map(RoleDocument::toDomain);
    }
}
