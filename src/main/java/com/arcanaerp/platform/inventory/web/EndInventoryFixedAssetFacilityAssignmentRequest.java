package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record EndInventoryFixedAssetFacilityAssignmentRequest(
    @NotBlank String thruDate,
    @NotBlank String reason,
    @NotBlank String endedBy
) {
}
