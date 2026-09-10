package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryPartyFixedAssetAssignmentView(
    UUID id,
    UUID inventoryPartyId,
    String partyCode,
    UUID inventoryFixedAssetId,
    String fixedAssetCode,
    Instant assignedFrom,
    Instant assignedThru,
    Long allocatedCostMoneyId,
    Instant createdAt
) {
}
