package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record EndInventoryFixedAssetPartyRoleAssignmentRequest(
    @NotBlank String thruDate,
    @NotBlank String reason,
    @NotBlank String endedBy
) {
}
