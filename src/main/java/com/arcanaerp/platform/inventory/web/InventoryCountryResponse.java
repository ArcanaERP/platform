package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryCountryResponse(
    UUID id,
    String code,
    String name,
    Instant createdAt
) {
}
