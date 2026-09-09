package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record UpdateInventoryFixedAssetTypeMetadataRequest(
    @NotBlank String code,
    @NotBlank String description,
    @NotBlank String changedBy
) {
}
