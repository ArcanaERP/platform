package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdjustInventoryRequest(
    @NotNull BigDecimal quantityDelta,
    @NotBlank String reason,
    @NotBlank String adjustedBy
) {
}
