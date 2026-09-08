package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryTelecomContactRequest(
    @NotBlank String ownerType,
    @NotBlank String ownerCode,
    @NotBlank String contactPurposeCode,
    @NotBlank String telecomType,
    String contactName,
    @NotBlank String contactValue
) {
}
