package com.ticketing.platform.application.model;

import java.util.UUID;

public record AuthResult(
    UUID userId,
    String email,
    String role,
    String token,
    String tokenType
) {
}
