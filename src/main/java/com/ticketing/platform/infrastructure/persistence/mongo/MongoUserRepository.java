package com.ticketing.platform.infrastructure.persistence.mongo;

import com.ticketing.platform.application.port.out.UserRepository;
import com.ticketing.platform.domain.model.AppUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@ConditionalOnProperty(prefix = "ticketing.adapters", name = "persistence", havingValue = "mongo", matchIfMissing = true)
public class MongoUserRepository implements UserRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoUserRepository(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<AppUser> create(AppUser user) {
        return mongoTemplate.insert(UserDocument.fromDomain(user))
            .map(UserDocument::toDomain);
    }

    @Override
    public Mono<AppUser> findByEmail(String email) {
        Query query = Query.query(Criteria.where("email").is(AppUser.normalizeEmail(email)));
        return mongoTemplate.findOne(query, UserDocument.class)
            .map(UserDocument::toDomain);
    }
}
