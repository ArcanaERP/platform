package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryItemLocationAssignmentRequest(
    @NotBlank String sku,
    @NotBlank String itemLocationCode,
    @NotBlank String assignedLocationCode,
    String assignedFacilityCode,
    String assignedStorageAreaCode,
    String validFrom,
    @NotBlank String assignedBy
) {
}
