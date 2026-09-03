package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryLocationView(
    UUID id,
    String code,
    String name,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
