package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetPartyRoleAssignmentView(
    UUID id,
    UUID inventoryFixedAssetId,
    String fixedAssetCode,
    String partyCode,
    String roleTypeCode,
    String comments,
    Instant fromDate,
    Instant thruDate,
    String assignedBy,
    Instant assignedAt
) {
}
