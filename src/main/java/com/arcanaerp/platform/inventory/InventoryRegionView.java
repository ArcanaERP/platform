package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryRegionView(
    UUID id,
    String countryCode,
    String code,
    String name,
    Instant createdAt
) {
}
