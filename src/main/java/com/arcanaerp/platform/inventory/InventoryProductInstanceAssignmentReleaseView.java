package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryProductInstanceAssignmentReleaseView(
    UUID id,
    UUID assignmentId,
    UUID inventoryItemId,
    String sku,
    String locationCode,
    String productInstanceCode,
    String reason,
    String releasedBy,
    Instant releasedAt
) {
}
