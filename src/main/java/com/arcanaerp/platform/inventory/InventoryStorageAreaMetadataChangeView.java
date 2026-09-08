package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryStorageAreaMetadataChangeView(
    UUID id,
    UUID storageAreaId,
    String facilityCode,
    String storageAreaCode,
    String previousName,
    String currentName,
    String previousStorageAreaType,
    String currentStorageAreaType,
    String previousParentStorageAreaCode,
    String currentParentStorageAreaCode,
    String changedBy,
    Instant changedAt
) {
}
