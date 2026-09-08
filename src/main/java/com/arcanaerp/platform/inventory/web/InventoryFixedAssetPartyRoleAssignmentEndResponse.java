package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetPartyRoleAssignmentEndResponse(
    UUID id,
    UUID assignmentId,
    UUID inventoryFixedAssetId,
    String fixedAssetCode,
    String partyCode,
    String roleTypeCode,
    Instant previousThruDate,
    Instant currentThruDate,
    String reason,
    String endedBy,
    Instant endedAt
) {
}
