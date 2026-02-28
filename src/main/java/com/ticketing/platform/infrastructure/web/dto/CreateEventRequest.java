package com.ticketing.platform.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateEventRequest(
    @NotBlank String name,
    @NotNull Instant date,
    @NotBlank String venue,
    @Min(1) int totalCapacity
) {
}
