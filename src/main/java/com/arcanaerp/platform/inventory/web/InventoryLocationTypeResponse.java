package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryLocationTypeResponse(
    UUID id,
    String code,
    String description,
    String parentCode,
    Instant createdAt
) {
}
