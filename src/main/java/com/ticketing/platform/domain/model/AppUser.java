package com.ticketing.platform.domain.model;

import com.ticketing.platform.domain.exception.DomainException;
import java.util.Locale;
import java.util.UUID;

public record AppUser(
    UUID id,
    String email,
    String passwordHash,
    String role
) {

    public AppUser {
        if (id == null) {
            throw new DomainException("User id is required");
        }
        if (email == null || email.isBlank()) {
            throw new DomainException("User email is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException("User password hash is required");
        }
        if (role == null || role.isBlank()) {
            throw new DomainException("User role is required");
        }
    }

    public static AppUser create(UUID id, String email, String passwordHash) {
        return new AppUser(
            id,
            normalizeEmail(email),
            passwordHash,
            "USER"
        );
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new DomainException("User email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
