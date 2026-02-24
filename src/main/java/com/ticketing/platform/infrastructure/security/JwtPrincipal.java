package com.ticketing.platform.infrastructure.security;

import java.util.UUID;

public record JwtPrincipal(
    UUID userId,
    String email,
    String role
) {
}
