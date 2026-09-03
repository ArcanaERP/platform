package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryLocationRequest(
    @NotBlank String code,
    @NotBlank String name
) {
}
