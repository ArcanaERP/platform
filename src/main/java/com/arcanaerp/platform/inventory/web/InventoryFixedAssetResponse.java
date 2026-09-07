package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetResponse(
    UUID id,
    String code,
    String description,
    String fixedAssetTypeCode,
    String comments,
    String externalIdentifier,
    String externalIdSource,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
