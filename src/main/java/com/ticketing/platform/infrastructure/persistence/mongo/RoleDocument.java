package com.ticketing.platform.infrastructure.persistence.mongo;

import com.ticketing.platform.domain.model.Role;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("roles")
public record RoleDocument(
        @Id String id,
        String name,
        Set<String> permissions) {

    public static RoleDocument fromDomain(Role role) {
        return new RoleDocument(
                role.id().toString(),
                role.name(),
                role.permissions() != null ? Set.copyOf(role.permissions()) : Set.of());
    }

    public Role toDomain() {
        return new Role(
                UUID.fromString(id),
                name,
                permissions != null ? Set.copyOf(permissions) : Set.of());
    }
}
