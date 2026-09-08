package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryCountryView(
    UUID id,
    String code,
    String name,
    Instant createdAt
) {
}
