package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryProductInstanceAssignmentView(
    UUID id,
    UUID inventoryItemId,
    String sku,
    String locationCode,
    String productInstanceCode,
    String assignedBy,
    Instant assignedAt
) {
}
