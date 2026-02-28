package com.ticketing.platform.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull UUID eventId,
    @NotBlank String customerId,
    @Min(1) int quantity
) {
}
