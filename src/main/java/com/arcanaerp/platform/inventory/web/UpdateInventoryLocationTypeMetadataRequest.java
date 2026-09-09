package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record UpdateInventoryLocationTypeMetadataRequest(
    @NotBlank String code,
    @NotBlank String description,
    String parentCode,
    @NotBlank String changedBy
) {
}
