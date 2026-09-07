package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetPartyRoleAssignmentResponse(
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
