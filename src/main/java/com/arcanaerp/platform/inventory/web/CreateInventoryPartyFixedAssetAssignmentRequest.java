package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryPartyFixedAssetAssignmentRequest(
    @NotBlank String partyCode,
    @NotBlank String fixedAssetCode,
    String assignedFrom,
    String assignedThru,
    Long allocatedCostMoneyId
) {
}
