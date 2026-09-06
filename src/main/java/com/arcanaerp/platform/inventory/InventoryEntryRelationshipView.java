package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryEntryRelationshipView(
    UUID id,
    String relationshipTypeCode,
    String fromSku,
    String fromLocationCode,
    String toSku,
    String toLocationCode,
    String fromRoleTypeCode,
    String toRoleTypeCode,
    String description,
    String statusCode,
    Instant createdAt
) {
}
