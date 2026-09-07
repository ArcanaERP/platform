package com.arcanaerp.platform.inventory;

import java.time.Instant;

public record RegisterInventoryEntryRelationshipCommand(
    String relationshipTypeCode,
    String fromSku,
    String fromLocationCode,
    String toSku,
    String toLocationCode,
    String fromRoleTypeCode,
    String toRoleTypeCode,
    String description,
    String statusCode,
    Instant fromDate,
    Instant thruDate
) {
}
