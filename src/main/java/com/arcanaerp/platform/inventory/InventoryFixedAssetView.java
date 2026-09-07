package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetView(
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
