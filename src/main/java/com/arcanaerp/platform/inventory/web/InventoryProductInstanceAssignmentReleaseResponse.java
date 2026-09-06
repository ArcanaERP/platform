package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryProductInstanceAssignmentReleaseResponse(
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
