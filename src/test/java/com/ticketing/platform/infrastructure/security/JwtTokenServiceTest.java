package com.ticketing.platform.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.platform.domain.model.AppUser;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class JwtTokenServiceTest {

    @Test
    void shouldGenerateAndParseToken() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getSecurity().getJwt().setSecret("this-is-a-long-test-secret-with-at-least-thirty-two-bytes");
        properties.getSecurity().getJwt().setExpirationMinutes(30);
        JwtTokenService jwtTokenService = new JwtTokenService(properties);

        AppUser user = AppUser.create(UUID.randomUUID(), "user@example.com", "hash");
        String token = jwtTokenService.generate(user).block();
        JwtPrincipal principal = jwtTokenService.parse(token).block();

        assertThat(token).isNotBlank();
        assertThat(principal.email()).isEqualTo("user@example.com");
        assertThat(principal.userId()).isEqualTo(user.id());
        assertThat(principal.roles()).containsExactly("USER");
    }

    @Test
    void shouldRejectInvalidToken() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getSecurity().getJwt().setSecret("this-is-a-long-test-secret-with-at-least-thirty-two-bytes");
        JwtTokenService jwtTokenService = new JwtTokenService(properties);

        assertThatThrownBy(() -> jwtTokenService.parse("invalid-token").block())
                .isInstanceOf(BadCredentialsException.class);
    }
}
