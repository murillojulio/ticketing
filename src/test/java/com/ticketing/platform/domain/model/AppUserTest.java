package com.ticketing.platform.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.platform.domain.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppUserTest {

    @Test
    void shouldCreateUserWithNormalizedEmailAndDefaultRole() {
        AppUser user = AppUser.create(
                UUID.randomUUID(),
                "User@Example.com",
                "password-hash");

        assertThat(user.email()).isEqualTo("user@example.com");
        assertThat(user.roles()).containsExactly("USER");
    }

    @Test
    void shouldRejectBlankEmailNormalization() {
        assertThatThrownBy(() -> AppUser.normalizeEmail(" "))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("email");
    }
}
