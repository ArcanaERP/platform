package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryFixedAssetFacilityAssignmentRequest(
    @NotBlank String fixedAssetCode,
    @NotBlank String facilityCode,
    @NotBlank String assignmentType,
    String comments,
    String fromDate,
    String thruDate,
    @NotBlank String assignedBy
) {
}
