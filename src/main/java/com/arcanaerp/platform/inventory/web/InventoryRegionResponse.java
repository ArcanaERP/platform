package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryRegionResponse(
    UUID id,
    String countryCode,
    String code,
    String name,
    Instant createdAt
) {
}
