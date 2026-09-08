package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record UpdateInventoryStorageAreaMetadataRequest(
    @NotBlank String name,
    @NotBlank String storageAreaType,
    String parentStorageAreaCode,
    @NotBlank String changedBy
) {
}
