package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemLocationAssignmentResponse(
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
