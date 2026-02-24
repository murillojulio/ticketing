package com.ticketing.platform.infrastructure.persistence.mongo;

import com.ticketing.platform.domain.model.AppUser;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public record UserDocument(
    @Id String id,
    String email,
    String passwordHash,
    String role
) {

    public static UserDocument fromDomain(AppUser user) {
        return new UserDocument(
            user.id().toString(),
            user.email(),
            user.passwordHash(),
            user.role()
        );
    }

    public AppUser toDomain() {
        return new AppUser(
            UUID.fromString(id),
            email,
            passwordHash,
            role
        );
    }
}
