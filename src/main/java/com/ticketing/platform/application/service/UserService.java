package com.ticketing.platform.application.service;

import com.ticketing.platform.application.port.in.UserUseCase;
import com.ticketing.platform.application.port.out.PasswordHasherPort;
import com.ticketing.platform.application.port.out.UserRepository;
import com.ticketing.platform.domain.exception.DomainException;
import com.ticketing.platform.domain.model.AppUser;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserService implements UserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasherPort passwordHasherPort;

    public UserService(UserRepository userRepository, PasswordHasherPort passwordHasherPort) {
        this.userRepository = userRepository;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    public Flux<AppUser> listUsers() {
        return userRepository.findAll();
    }

    @Override
    public Mono<AppUser> getUser(UUID id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new DomainException("User not found")));
    }

    @Override
    public Mono<AppUser> createUser(CreateUserCommand command) {
        String normalizedEmail = AppUser.normalizeEmail(command.email());
        return userRepository.findByEmail(normalizedEmail)
                .flatMap(existing -> Mono.<AppUser>error(new DomainException("User already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    AppUser newUser = new AppUser(
                            UUID.randomUUID(),
                            normalizedEmail,
                            passwordHasherPort.hash(command.password()),
                            command.roles());
                    return userRepository.create(newUser);
                }));
    }

    @Override
    public Mono<AppUser> updateUser(UpdateUserCommand command) {
        return userRepository.findById(command.id())
                .switchIfEmpty(Mono.error(new DomainException("User not found")))
                .flatMap(existingUser -> {
                    AppUser updatedUser = new AppUser(
                            existingUser.id(),
                            AppUser.normalizeEmail(command.email()),
                            existingUser.passwordHash(),
                            command.roles());
                    return userRepository.update(updatedUser);
                });
    }

    @Override
    public Mono<Void> deleteUser(UUID id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new DomainException("User not found")))
                .flatMap(user -> userRepository.deleteById(id));
    }
}
