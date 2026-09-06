package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryEntryRelationshipTypeView(
    UUID id,
    String code,
    String description,
    String comments,
    Instant createdAt
) {
}
