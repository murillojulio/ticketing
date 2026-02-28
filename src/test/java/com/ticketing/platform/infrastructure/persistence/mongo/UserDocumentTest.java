package com.ticketing.platform.infrastructure.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.platform.domain.model.AppUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserDocumentTest {

    @Test
    void shouldMapUserBetweenDomainAndDocument() {
        AppUser domain = AppUser.create(UUID.randomUUID(), "user@example.com", "hash");

        UserDocument document = UserDocument.fromDomain(domain);
        AppUser restored = document.toDomain();

        assertThat(document.email()).isEqualTo(domain.email());
        assertThat(restored).isEqualTo(domain);
    }
}
