package com.arcanaerp.platform.inventory;

import java.time.Instant;

public record RegisterInventoryItemLocationAssignmentCommand(
    String sku,
    String itemLocationCode,
    String assignedLocationCode,
    Instant validFrom,
    String assignedBy
) {
}
