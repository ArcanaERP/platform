package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryFixedAssetTypeRequest(
    @NotBlank String code,
    @NotBlank String description
) {
}
