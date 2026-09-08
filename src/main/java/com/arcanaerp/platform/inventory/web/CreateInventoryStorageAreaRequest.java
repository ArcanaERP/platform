package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryStorageAreaRequest(
    @NotBlank String facilityCode,
    @NotBlank String code,
    @NotBlank String name,
    @NotBlank String storageAreaType,
    String parentStorageAreaCode
) {
}
