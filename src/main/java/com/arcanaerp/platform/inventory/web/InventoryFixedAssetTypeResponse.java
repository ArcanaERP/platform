package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetTypeResponse(
    UUID id,
    String code,
    String description,
    Instant createdAt
) {
}
