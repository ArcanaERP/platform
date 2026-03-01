package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferInventoryRequest(
    @NotBlank String sourceLocationCode,
    @NotBlank String destinationLocationCode,
    @NotNull BigDecimal quantity,
    @NotBlank String reason,
    @NotBlank String adjustedBy
) {
}
