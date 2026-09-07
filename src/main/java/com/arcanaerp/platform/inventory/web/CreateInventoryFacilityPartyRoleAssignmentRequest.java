package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryFacilityPartyRoleAssignmentRequest(
    @NotBlank String facilityCode,
    @NotBlank String partyCode,
    @NotBlank String roleTypeCode,
    String comments,
    String fromDate,
    String thruDate,
    @NotBlank String assignedBy
) {
}
