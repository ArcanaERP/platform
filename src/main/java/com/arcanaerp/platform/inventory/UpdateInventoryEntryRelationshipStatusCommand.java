package com.arcanaerp.platform.inventory;

import java.util.UUID;

public record UpdateInventoryEntryRelationshipStatusCommand(
    UUID id,
    String statusCode,
    String reason,
    String changedBy
) {
}
