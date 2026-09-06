package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryEntryRelationshipStatusChangeResponse(
    UUID id,
    UUID relationshipId,
    String previousStatusCode,
    String currentStatusCode,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
