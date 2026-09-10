package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryPartyFixedAssetAssignmentResponse(
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
