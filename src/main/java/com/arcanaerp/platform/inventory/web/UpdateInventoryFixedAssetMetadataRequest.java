package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record UpdateInventoryFixedAssetMetadataRequest(
    @NotBlank String description,
    String fixedAssetTypeCode,
    String comments,
    String externalIdentifier,
    String externalIdSource,
    @NotBlank String changedBy
) {
}
