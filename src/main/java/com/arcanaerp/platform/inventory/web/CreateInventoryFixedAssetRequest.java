package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryFixedAssetRequest(
    @NotBlank String code,
    @NotBlank String description,
    String fixedAssetTypeCode,
    String comments,
    String externalIdentifier,
    String externalIdSource
) {
}
