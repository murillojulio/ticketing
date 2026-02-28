package com.ticketing.platform.application.model;

import java.util.Set;
import java.util.UUID;

public record AuthResult(
        UUID userId,
        String email,
        Set<String> roles,
        String token,
        String tokenType) {
}
