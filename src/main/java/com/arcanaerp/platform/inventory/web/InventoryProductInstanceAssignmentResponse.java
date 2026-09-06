package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryProductInstanceAssignmentResponse(
    UUID id,
    UUID inventoryItemId,
    String sku,
    String locationCode,
    String productInstanceCode,
    String assignedBy,
    Instant assignedAt
) {
}
