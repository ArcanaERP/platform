package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryFixedAssetPartyRoleAssignmentRequest(
    @NotBlank String fixedAssetCode,
    @NotBlank String partyCode,
    @NotBlank String roleTypeCode,
    String comments,
    String fromDate,
    String thruDate,
    @NotBlank String assignedBy
) {
}
