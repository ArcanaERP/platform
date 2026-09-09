package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetFacilityAssignmentEndView(
    UUID id,
    UUID assignmentId,
    UUID inventoryFixedAssetId,
    UUID inventoryFacilityId,
    String fixedAssetCode,
    String facilityCode,
    String assignmentType,
    Instant previousThruDate,
    Instant currentThruDate,
    String reason,
    String endedBy,
    Instant endedAt
) {
}
