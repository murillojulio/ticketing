package com.ticketing.platform.infrastructure.config;

import com.ticketing.platform.application.port.out.RoleRepository;
import com.ticketing.platform.domain.model.Role;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RoleInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Initialize default roles
        initRole("ADMIN", Set.of("role:read", "role:write", "user_role:write", "event:read", "event:write",
                "order:read", "order:write")).subscribe();
        initRole("USER", Set.of("event:read", "order:read", "order:write")).subscribe();
    }

    private Mono<Role> initRole(String name, Set<String> permissions) {
        return roleRepository.findByName(name)
                .switchIfEmpty(Mono.defer(() -> {
                    Role role = new Role(UUID.randomUUID(), name, permissions);
                    return roleRepository.create(role);
                }));
    }
}
