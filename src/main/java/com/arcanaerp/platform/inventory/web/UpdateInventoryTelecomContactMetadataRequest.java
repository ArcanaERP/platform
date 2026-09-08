package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record UpdateInventoryTelecomContactMetadataRequest(
    @NotBlank String contactPurposeCode,
    @NotBlank String telecomType,
    String contactName,
    @NotBlank String contactValue,
    @NotBlank String changedBy
) {
}
