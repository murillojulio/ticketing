package com.ticketing.platform.infrastructure.web.dto;

import com.ticketing.platform.application.model.AuthResult;
import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        Set<String> roles,
        String token,
        String tokenType) {

    public static AuthResponse from(AuthResult authResult) {
        return new AuthResponse(
                authResult.userId(),
                authResult.email(),
                authResult.roles(),
                authResult.token(),
                authResult.tokenType());
    }
}
