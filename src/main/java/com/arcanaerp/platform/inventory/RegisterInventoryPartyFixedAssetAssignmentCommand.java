package com.arcanaerp.platform.inventory;

import java.time.Instant;

public record RegisterInventoryPartyFixedAssetAssignmentCommand(
    String partyCode,
    String fixedAssetCode,
    Instant assignedFrom,
    Instant assignedThru,
    Long allocatedCostMoneyId
) {
}
