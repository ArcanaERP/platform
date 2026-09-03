package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotNull;

public record UpdateInventoryLocationActiveRequest(
    @NotNull Boolean active
) {
}
