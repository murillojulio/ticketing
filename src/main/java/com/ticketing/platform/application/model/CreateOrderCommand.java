package com.ticketing.platform.application.model;

import java.util.UUID;

public record CreateOrderCommand(
    UUID eventId,
    String customerId,
    int quantity
) {
}
