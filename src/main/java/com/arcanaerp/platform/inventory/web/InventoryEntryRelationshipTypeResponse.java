package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryEntryRelationshipTypeResponse(
    UUID id,
    String code,
    String description,
    String comments,
    Instant createdAt
) {
}
