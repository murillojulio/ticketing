package com.ticketing.platform.infrastructure.web.dto;

import com.ticketing.platform.application.model.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record PaymentWebhookRequest(
    @NotNull UUID orderId,
    @NotBlank String paymentId,
    @NotNull PaymentStatus status,
    Instant occurredAt
) {
}
