package com.ticketing.platform.application.model;

public record LoginUserCommand(
    String email,
    String password
) {
}
