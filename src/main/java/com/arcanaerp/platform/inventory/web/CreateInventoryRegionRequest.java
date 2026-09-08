package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryRegionRequest(
    @NotBlank String countryCode,
    @NotBlank String code,
    @NotBlank String name
) {
}
