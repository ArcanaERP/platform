package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateInventoryStorageAreaActiveRequest(
    @NotNull Boolean active,
    @NotBlank String changedBy
) {
}
