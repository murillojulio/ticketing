package com.ticketing.platform.domain.model;

import com.ticketing.platform.domain.exception.DomainException;
import java.util.Set;
import java.util.UUID;

public record Role(
        UUID id,
        String name,
        Set<String> permissions) {
    public Role {
        if (id == null) {
            throw new DomainException("Role id is required");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("Role name is required");
        }
        if (permissions == null) {
            permissions = Set.of();
        }
    }
}
