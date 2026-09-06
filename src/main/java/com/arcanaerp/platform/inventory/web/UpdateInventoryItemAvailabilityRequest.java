package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateInventoryItemAvailabilityRequest(
    @NotNull BigDecimal availableQuantityDelta,
    @NotNull BigDecimal soldQuantityDelta,
    @NotBlank String reason,
    @NotBlank String changedBy
) {
}
