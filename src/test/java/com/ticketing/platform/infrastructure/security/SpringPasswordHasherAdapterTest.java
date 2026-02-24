package com.ticketing.platform.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SpringPasswordHasherAdapterTest {

    @Test
    void shouldHashAndVerifyPassword() {
        SpringPasswordHasherAdapter adapter = new SpringPasswordHasherAdapter(new BCryptPasswordEncoder());

        String hash = adapter.hash("password123");

        assertThat(hash).isNotBlank();
        assertThat(adapter.matches("password123", hash)).isTrue();
        assertThat(adapter.matches("wrong-password", hash)).isFalse();
    }
}
