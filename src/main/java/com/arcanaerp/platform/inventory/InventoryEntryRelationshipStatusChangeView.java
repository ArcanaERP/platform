package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryEntryRelationshipStatusChangeView(
    UUID id,
    UUID relationshipId,
    String previousStatusCode,
    String currentStatusCode,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
