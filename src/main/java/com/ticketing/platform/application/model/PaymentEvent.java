package com.ticketing.platform.application.model;

import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(
    UUID orderId,
    String paymentId,
    PaymentStatus status,
    Instant occurredAt
) {
}
