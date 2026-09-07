package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemLocationAssignmentEndResponse(
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
