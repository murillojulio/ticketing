package com.ticketing.platform.infrastructure.persistence.inmemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.platform.domain.model.AppUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryUserRepositoryTest {

    @Test
    void shouldCreateAndFindUserByEmail() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        AppUser user = AppUser.create(UUID.randomUUID(), "user@example.com", "hash");

        repository.create(user).block();
        AppUser found = repository.findByEmail("USER@EXAMPLE.COM").block();

        assertThat(found).isEqualTo(user);
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        AppUser first = AppUser.create(UUID.randomUUID(), "user@example.com", "hash");
        AppUser second = AppUser.create(UUID.randomUUID(), "user@example.com", "hash2");
        repository.create(first).block();

        assertThatThrownBy(() -> repository.create(second).block()).isInstanceOf(RuntimeException.class);
    }
}
