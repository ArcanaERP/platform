package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryLocationTypeView(
    UUID id,
    String code,
    String description,
    String parentCode,
    Instant createdAt
) {
}
