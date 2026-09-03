package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryLocationResponse(
    UUID id,
    String code,
    String name,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
