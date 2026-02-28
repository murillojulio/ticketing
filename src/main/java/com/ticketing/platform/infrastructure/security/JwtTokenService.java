package com.ticketing.platform.infrastructure.security;

import com.ticketing.platform.application.port.out.AuthTokenPort;
import com.ticketing.platform.domain.model.AppUser;
import com.ticketing.platform.infrastructure.config.ApplicationProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JwtTokenService implements AuthTokenPort {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtTokenService(ApplicationProperties applicationProperties) {
        String secret = applicationProperties.getSecurity().getJwt().getSecret();
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = applicationProperties.getSecurity().getJwt().getExpirationMinutes();
    }

    @Override
    public Mono<String> generate(AppUser user) {
        return Mono.fromSupplier(() -> {
            Instant now = Instant.now();
            Instant expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES);
            return Jwts.builder()
                    .subject(user.email())
                    .claim("uid", user.id().toString())
                    .claim("roles", user.roles())
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiration))
                    .signWith(secretKey)
                    .compact();
        });
    }

    public Mono<JwtPrincipal> parse(String token) {
        return Mono.fromSupplier(() -> {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String email = claims.getSubject();
            String userId = claims.get("uid", String.class);
            List<String> rolesList = claims.get("roles", List.class);
            return new JwtPrincipal(
                    UUID.fromString(userId),
                    email,
                    rolesList != null ? Set.copyOf(rolesList) : Set.of());
        })
                .onErrorMap(
                        error -> error instanceof JwtException || error instanceof IllegalArgumentException,
                        error -> new BadCredentialsException("Invalid JWT token", error));
    }
}
