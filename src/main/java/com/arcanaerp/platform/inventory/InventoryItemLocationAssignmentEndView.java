package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemLocationAssignmentEndView(
    UUID id,
    UUID assignmentId,
    UUID inventoryItemId,
    String sku,
    String itemLocationCode,
    String assignedLocationCode,
    Instant previousValidThru,
    Instant currentValidThru,
    String reason,
    String endedBy,
    Instant endedAt
) {
}
