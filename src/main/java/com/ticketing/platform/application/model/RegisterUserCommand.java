package com.ticketing.platform.application.model;

public record RegisterUserCommand(
    String email,
    String password
) {
}
