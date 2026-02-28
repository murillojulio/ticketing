package com.ticketing.platform.application.service;

import com.ticketing.platform.application.port.in.UserRoleUseCase;
import com.ticketing.platform.application.port.out.RoleRepository;
import com.ticketing.platform.application.port.out.UserRepository;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.model.AppUser;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserRoleService implements UserRoleUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserRoleService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public Mono<AppUser> assignRoles(UUID userId, Set<String> roles) {
        return checkRolesExist(roles)
                .then(userRepository.findById(userId))
                .switchIfEmpty(Mono.error(new DomainException("User not found")))
                .flatMap(user -> {
                    Set<String> newRoles = new HashSet<>(user.roles());
                    newRoles.addAll(roles);

                    AppUser updatedUser = new AppUser(
                            user.id(),
                            user.email(),
                            user.passwordHash(),
                            newRoles);
                    return userRepository.update(updatedUser);
                });
    }

    @Override
    public Mono<AppUser> removeRoles(UUID userId, Set<String> roles) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new DomainException("User not found")))
                .flatMap(user -> {
                    Set<String> newRoles = new HashSet<>(user.roles());
                    newRoles.removeAll(roles);
                    if (newRoles.isEmpty()) {
                        return Mono.error(new DomainException("User must have at least one role"));
                    }

                    AppUser updatedUser = new AppUser(
                            user.id(),
                            user.email(),
                            user.passwordHash(),
                            newRoles);
                    return userRepository.update(updatedUser);
                });
    }

    private Mono<Void> checkRolesExist(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(roles)
                .flatMap(roleRepository::findByName)
                .collectList()
                .flatMap(foundRoles -> {
                    if (foundRoles.size() != roles.size()) {
                        return Mono.error(new DomainException("One or more roles do not exist"));
                    }
                    return Mono.empty();
                });
    }
}
