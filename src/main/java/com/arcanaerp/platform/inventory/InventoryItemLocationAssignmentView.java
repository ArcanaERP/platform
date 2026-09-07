package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemLocationAssignmentView(
    UUID id,
    UUID inventoryItemId,
    String sku,
    String itemLocationCode,
    String assignedLocationCode,
    Instant validFrom,
    Instant validThru,
    boolean active,
    String assignedBy,
    Instant assignedAt,
    String endReason,
    String endedBy,
    Instant endedAt
) {
}
