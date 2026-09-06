package com.arcanaerp.platform.inventory;

public record RegisterInventoryEntryRelationshipCommand(
    String relationshipTypeCode,
    String fromSku,
    String fromLocationCode,
    String toSku,
    String toLocationCode,
    String fromRoleTypeCode,
    String toRoleTypeCode,
    String description,
    String statusCode
) {
}
