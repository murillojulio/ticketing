package com.ticketing.platform.infrastructure.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ticketing.platform.domain.model.AppUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class MongoUserRepositoryTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @Test
    void shouldCreateAndFindUserByEmail() {
        MongoUserRepository repository = new MongoUserRepository(mongoTemplate);
        AppUser user = AppUser.create(UUID.randomUUID(), "user@example.com", "hash");
        UserDocument document = UserDocument.fromDomain(user);

        when(mongoTemplate.insert(any(UserDocument.class))).thenReturn(Mono.just(document));
        when(mongoTemplate.findOne(any(), any(Class.class))).thenReturn(Mono.just(document));

        AppUser created = repository.create(user).block();
        AppUser found = repository.findByEmail("USER@EXAMPLE.COM").block();

        assertThat(created).isEqualTo(user);
        assertThat(found).isEqualTo(user);
    }
}
